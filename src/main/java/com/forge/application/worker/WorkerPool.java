package com.forge.application.worker;

import com.forge.application.metrics.ForgeMetrics;
import com.forge.application.service.BatchService;
import com.forge.domain.executor.JobExecutorRegistry;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class WorkerPool {

    private final ExecutorService pool;
    private final JobQueue jobQueue;
    private final JobExecutorRegistry jobExecutorRegistry;
    private final JobStateManager jobStateManager;
    private final BatchService batchService;
    private final ClientConcurrencyLimiter concurrencyLimiter;
    private final JobChainExecutor jobChainExecutor;
    private final RetryScheduler retryScheduler;
    private final ForgeMetrics forgeMetrics;
    private final int poolSize;

    // volatile: only main thread writes, worker threads read — no CAS needed
    private volatile boolean running = false;

    public WorkerPool(@Value("${forge.worker.pool-size}") int poolSize,
                      JobQueue jobQueue,
                      JobExecutorRegistry jobExecutorRegistry,
                      JobStateManager jobStateManager, BatchService batchService,
                      ClientConcurrencyLimiter concurrencyLimiter, JobChainExecutor jobChainExecutor,
                      RetryScheduler retryScheduler, ForgeMetrics forgeMetrics) {
        this.batchService = batchService;
        this.concurrencyLimiter = concurrencyLimiter;
        this.jobChainExecutor = jobChainExecutor;
        this.retryScheduler = retryScheduler;
        this.forgeMetrics = forgeMetrics;
        this.poolSize = poolSize;
        this.jobQueue = jobQueue;
        this.jobExecutorRegistry = jobExecutorRegistry;
        this.jobStateManager = jobStateManager;
        this.pool = Executors.newFixedThreadPool(poolSize);
    }

    @PostConstruct
    public void start() {
        running = true;
        for (int i = 0; i < poolSize; i++) {
            pool.submit(new JobWorker(jobQueue, this, jobExecutorRegistry, jobStateManager,
                    batchService, concurrencyLimiter, jobChainExecutor, retryScheduler, forgeMetrics));
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        pool.shutdownNow();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getActiveCount() {
        // cast is safe — newFixedThreadPool returns a ThreadPoolExecutor
        return ((java.util.concurrent.ThreadPoolExecutor) pool).getActiveCount();
    }
}
