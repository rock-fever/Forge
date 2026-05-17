package com.forge.application.metrics;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class ForgeMetricsTest {

    @Test
    void counters_areThreadSafe_underConcurrentIncrements() throws InterruptedException {
        ForgeMetrics metrics = new ForgeMetrics();
        int threads = 10;
        int incrementsPerThread = 1000;

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        metrics.jobSubmitted();
                        metrics.jobCompleted();
                        metrics.jobFailed();
                        metrics.jobRetried();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        done.await();
        pool.shutdown();

        long expected = (long) threads * incrementsPerThread;
        assertThat(metrics.getJobsSubmitted()).isEqualTo(expected);
        assertThat(metrics.getJobsCompleted()).isEqualTo(expected);
        assertThat(metrics.getJobsFailed()).isEqualTo(expected);
        assertThat(metrics.getJobsRetried()).isEqualTo(expected);
    }
}
