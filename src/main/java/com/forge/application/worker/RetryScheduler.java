package com.forge.application.worker;

import com.forge.application.metrics.ForgeMetrics;
import com.forge.domain.model.Job;
import com.forge.domain.repository.IJobRepository;
import com.forge.shared.exception.JobNotFoundException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RetryScheduler {

    private final ScheduledExecutorService scheduler;
    private final IJobRepository jobRepository;
    private final JobStateManager jobStateManager;
    private final ForgeMetrics forgeMetrics;
    private final long initialDelayMs;
    private final double backoffMultiplier;

    @Autowired
    public RetryScheduler(IJobRepository jobRepository,
                          JobStateManager jobStateManager,
                          ForgeMetrics forgeMetrics,
                          @Value("${forge.retry.initial-delay-ms:1000}") long initialDelayMs,
                          @Value("${forge.retry.backoff-multiplier:2.0}") double backoffMultiplier) {
        this(jobRepository, jobStateManager, forgeMetrics, initialDelayMs, backoffMultiplier,
                Executors.newScheduledThreadPool(1));
    }

    // Package-private: used by tests to inject a mock scheduler
    RetryScheduler(IJobRepository jobRepository,
                   JobStateManager jobStateManager,
                   ForgeMetrics forgeMetrics,
                   long initialDelayMs,
                   double backoffMultiplier,
                   ScheduledExecutorService scheduler) {
        this.jobRepository = jobRepository;
        this.jobStateManager = jobStateManager;
        this.forgeMetrics = forgeMetrics;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.scheduler = scheduler;
    }

    /**
     * If the job has retries remaining, flips it to RETRYING immediately and
     * schedules a task to flip it back to PENDING after the backoff delay.
     * The dispatcher picks it up again once it is PENDING.
     */
    public void scheduleIfEligible(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getRetryCount() >= job.getMaxRetries()) return;

        long delayMs = computeDelay(job.getRetryCount());
        OffsetDateTime retryAt = OffsetDateTime.now().plusNanos(delayMs * 1_000_000L);

        // FAILED → RETRYING: increments retryCount and records retryAt for observability
        jobStateManager.markRetrying(jobId, retryAt);
        forgeMetrics.jobRetried();

        // After the delay, flip RETRYING → PENDING so the dispatcher picks it up
        scheduler.schedule(
                () -> jobStateManager.markPendingForRetry(jobId),
                delayMs,
                TimeUnit.MILLISECONDS
        );
    }

    // Visible for testing
    long computeDelay(int currentRetryCount) {
        return (long) (initialDelayMs * Math.pow(backoffMultiplier, currentRetryCount));
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
