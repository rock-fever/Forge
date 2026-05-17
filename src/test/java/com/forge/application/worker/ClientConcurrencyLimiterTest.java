package com.forge.application.worker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConcurrencyLimiterTest {

    ClientConcurrencyLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new ClientConcurrencyLimiter();
    }

    @Test
    void acquire_blocksThirdThread_whenLimitIsTwo() throws InterruptedException {
        UUID clientId = UUID.randomUUID();
        int limit = 2;

        // Acquire both permits — slots are now full
        limiter.acquire(clientId, limit);
        limiter.acquire(clientId, limit);

        AtomicInteger acquiredCount = new AtomicInteger(0);
        CountDownLatch thirdThreadStarted = new CountDownLatch(1);
        CountDownLatch thirdThreadAcquired = new CountDownLatch(1);

        Thread thirdThread = new Thread(() -> {
            thirdThreadStarted.countDown();
            limiter.acquire(clientId, limit); // should block here
            acquiredCount.incrementAndGet();
            thirdThreadAcquired.countDown();
        });
        thirdThread.start();

        thirdThreadStarted.await(); // wait until 3rd thread is trying to acquire
        Thread.sleep(100);          // give it time to confirm it is blocked

        assertThat(acquiredCount.get()).isEqualTo(0); // still blocked

        limiter.release(clientId); // free one slot

        thirdThreadAcquired.await(); // wait for 3rd thread to unblock
        assertThat(acquiredCount.get()).isEqualTo(1); // now unblocked

        thirdThread.join();
    }

    @Test
    void acquire_allowsConcurrentJobs_upToLimit() throws InterruptedException {
        UUID clientId = UUID.randomUUID();
        int limit = 3;
        int threadCount = 3;

        CountDownLatch allAcquired = new CountDownLatch(threadCount);
        CountDownLatch allDone = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                limiter.acquire(clientId, limit);
                allAcquired.countDown();
                try {
                    allDone.await(); // hold the permit until all threads have acquired
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    limiter.release(clientId);
                }
            });
        }

        // All 3 threads should acquire without blocking since limit == threadCount
        boolean allAcquiredInTime = allAcquired.await(2, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(allAcquiredInTime).isTrue();

        // Release all threads
        for (int i = 0; i < threadCount; i++) allDone.countDown();
        pool.shutdown();
    }

    @Test
    void release_doesNotThrow_whenNoSemaphoreExists() {
        // Releasing a clientId that never acquired should not throw
        org.assertj.core.api.Assertions.assertThatCode(
                () -> limiter.release(UUID.randomUUID()))
                .doesNotThrowAnyException();
    }
}
