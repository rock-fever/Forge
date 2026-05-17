package com.forge.application.worker;

import com.forge.application.metrics.ForgeMetrics;
import com.forge.application.service.BatchService;
import com.forge.domain.executor.JobExecutorRegistry;
import com.forge.domain.executor.JobResult;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.shared.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public class JobWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final JobQueue jobQueue;
    private final WorkerPool workerPool;
    private final JobExecutorRegistry jobExecutorRegistry;
    private final JobStateManager jobStateManager;
    private final BatchService batchService;
    private final ClientConcurrencyLimiter concurrencyLimiter;
    private final JobChainExecutor jobChainExecutor;
    private final RetryScheduler retryScheduler;
    private final ForgeMetrics forgeMetrics;

    public JobWorker(JobQueue jobQueue, WorkerPool workerPool,
            JobExecutorRegistry jobExecutorRegistry,
            JobStateManager jobStateManager, BatchService batchService,
            ClientConcurrencyLimiter concurrencyLimiter, JobChainExecutor jobChainExecutor,
            RetryScheduler retryScheduler, ForgeMetrics forgeMetrics) {
        this.jobQueue = jobQueue;
        this.workerPool = workerPool;
        this.jobExecutorRegistry = jobExecutorRegistry;
        this.jobStateManager = jobStateManager;
        this.batchService = batchService;
        this.concurrencyLimiter = concurrencyLimiter;
        this.jobChainExecutor = jobChainExecutor;
        this.retryScheduler = retryScheduler;
        this.forgeMetrics = forgeMetrics;
    }

    @Override
    public void run() {
        while (workerPool.isRunning()) {
            try {
                Job job = jobQueue.take();
                execute(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // An unexpected exception from execute() (e.g. DB row deleted mid-flight).
                // Log and continue the loop — one bad job must not kill the worker thread.
                log.error("Unexpected error processing job, worker continuing", e);
            }
        }
    }

    private void execute(Job job) {
        UUID jobId = job.getId();
        UUID clientId = job.getClient().getId();
        int maxJobs = job.getClient().getMaxConcurrentJobs();

        // Store job ID in ThreadLocal so MDC picks it up on all log lines from this thread.
        JobContext.set(jobId);
        MDC.put("jobId", String.valueOf(jobId));
        try {
            concurrencyLimiter.acquire(clientId, maxJobs);
            try {
                jobStateManager.markRunning(jobId);
                JobStatus finalStatus;
                try {
                    JobResult result = jobExecutorRegistry.get(job.getType()).execute(job);
                    if (result.success()) {
                        jobStateManager.markDone(jobId, result.output());
                        finalStatus = JobStatus.DONE;
                    } else {
                        jobStateManager.markFailed(jobId, result.output());
                        finalStatus = JobStatus.FAILED;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    jobStateManager.markFailed(jobId, "Interrupted");
                    finalStatus = JobStatus.FAILED;
                } catch (Exception e) {
                    jobStateManager.markFailed(jobId, e.getMessage());
                    finalStatus = JobStatus.FAILED;
                }

                // If the job failed and still has retries, schedule a retry and stop here.
                // Dependents and the batch latch must not be notified until the final outcome.
                boolean willRetry = finalStatus == JobStatus.FAILED
                        && job.getRetryCount() < job.getMaxRetries();
                if (willRetry) {
                    retryScheduler.scheduleIfEligible(jobId);
                    return;
                }

                // Job is truly done (DONE or permanently FAILED): update metrics, notify dependents and batch.
                if (finalStatus == JobStatus.DONE) {
                    forgeMetrics.jobCompleted();
                } else {
                    forgeMetrics.jobFailed();
                }
                jobChainExecutor.onJobCompleted(jobId, finalStatus);
                if (job.getBatchId() != null) {
                    batchService.jobFinished(job.getBatchId(), finalStatus);
                }
            } finally {
                concurrencyLimiter.release(clientId);
            }
        } finally {
            // Always clear ThreadLocal and MDC before the thread returns to the pool.
            // Forgetting this causes the next job on this thread to log with a stale jobId.
            JobContext.clear();
            MDC.remove("jobId");
        }
    }
}
