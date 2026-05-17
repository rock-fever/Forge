package com.forge.application.worker;

import com.forge.application.metrics.ForgeMetrics;
import com.forge.application.service.BatchService;
import com.forge.domain.executor.JobExecutor;
import com.forge.domain.executor.JobExecutorRegistry;
import com.forge.domain.executor.JobResult;
import com.forge.domain.model.Client;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.model.enums.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobWorkerTest {

    @Mock JobQueue jobQueue;
    @Mock WorkerPool workerPool;
    @Mock JobExecutorRegistry executorRegistry;
    @Mock JobExecutor jobExecutor;
    @Mock JobStateManager jobStateManager;
    @Mock BatchService batchService;
    @Mock ClientConcurrencyLimiter concurrencyLimiter;
    @Mock JobChainExecutor jobChainExecutor;
    @Mock RetryScheduler retryScheduler;
    @Mock ForgeMetrics forgeMetrics;

    JobWorker jobWorker;

    Client client = Client.builder().name("Acme").apiKey("key-1").maxConcurrentJobs(5).build();

    @BeforeEach
    void setUp() {
        jobWorker = new JobWorker(jobQueue, workerPool, executorRegistry, jobStateManager,
                batchService, concurrencyLimiter, jobChainExecutor, retryScheduler, forgeMetrics);
    }

    @Test
    void execute_callsMarkRunningThenMarkDone_onSuccess() throws InterruptedException {
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        UUID jobId = job.getId();
        when(jobQueue.take()).thenReturn(job).thenThrow(InterruptedException.class);
        when(workerPool.isRunning()).thenReturn(true);
        when(executorRegistry.get(JobType.EMAIL)).thenReturn(jobExecutor);
        when(jobExecutor.execute(job)).thenReturn(JobResult.success("sent"));

        jobWorker.run();

        verify(concurrencyLimiter).acquire(client.getId(), 5);
        verify(jobStateManager).markRunning(jobId);
        verify(jobStateManager).markDone(jobId, "sent");
        verify(jobStateManager, never()).markFailed(any(), any());
        verify(jobChainExecutor).onJobCompleted(jobId, JobStatus.DONE);
        verify(forgeMetrics).jobCompleted();
        verify(retryScheduler, never()).scheduleIfEligible(any());
        verify(concurrencyLimiter).release(client.getId());
    }

    @Test
    void execute_schedulesRetry_whenExecutorReturnsFailureAndRetriesRemain() throws InterruptedException {
        // default maxRetries=3, retryCount=0 → eligible for retry
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        UUID jobId = job.getId();
        when(jobQueue.take()).thenReturn(job).thenThrow(InterruptedException.class);
        when(workerPool.isRunning()).thenReturn(true);
        when(executorRegistry.get(JobType.EMAIL)).thenReturn(jobExecutor);
        when(jobExecutor.execute(job)).thenReturn(JobResult.failure("SMTP error"));

        jobWorker.run();

        verify(jobStateManager).markRunning(jobId);
        verify(jobStateManager).markFailed(jobId, "SMTP error");
        verify(retryScheduler).scheduleIfEligible(jobId);
        // chain and batch must NOT be notified — outcome is not final yet
        verify(jobChainExecutor, never()).onJobCompleted(any(), any());
        verify(batchService, never()).jobFinished(any(), any());
        verify(concurrencyLimiter).release(client.getId());
    }

    @Test
    void execute_callsChainExecutor_whenJobFailsPermanently() throws InterruptedException {
        // maxRetries=0 → no retries, failure is permanent
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").maxRetries(0).build();
        UUID jobId = job.getId();
        when(jobQueue.take()).thenReturn(job).thenThrow(InterruptedException.class);
        when(workerPool.isRunning()).thenReturn(true);
        when(executorRegistry.get(JobType.EMAIL)).thenReturn(jobExecutor);
        when(jobExecutor.execute(job)).thenReturn(JobResult.failure("SMTP error"));

        jobWorker.run();

        verify(jobStateManager).markFailed(jobId, "SMTP error");
        verify(jobChainExecutor).onJobCompleted(jobId, JobStatus.FAILED);
        verify(forgeMetrics).jobFailed();
        verify(retryScheduler, never()).scheduleIfEligible(any());
        verify(concurrencyLimiter).release(client.getId());
    }

    @Test
    void execute_schedulesRetry_evenWhenExecutorThrows() throws InterruptedException {
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        UUID jobId = job.getId();
        when(jobQueue.take()).thenReturn(job).thenThrow(InterruptedException.class);
        when(workerPool.isRunning()).thenReturn(true);
        when(executorRegistry.get(JobType.EMAIL)).thenReturn(jobExecutor);
        when(jobExecutor.execute(job)).thenThrow(new RuntimeException("connection refused"));

        jobWorker.run();

        verify(jobStateManager).markFailed(jobId, "connection refused");
        verify(retryScheduler).scheduleIfEligible(jobId);
        // permit must be released even when executor throws
        verify(concurrencyLimiter).release(client.getId());
    }
}
