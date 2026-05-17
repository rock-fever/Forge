package com.forge.application.worker;

import com.forge.domain.model.Client;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.model.enums.JobType;
import com.forge.domain.repository.IJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobStateManagerTest {

    @Mock
    IJobRepository jobRepository;

    JobStateManager jobStateManager;

    Client client = Client.builder().name("Acme").apiKey("key-1").build();

    @BeforeEach
    void setUp() {
        jobStateManager = new JobStateManager(jobRepository);
    }

    @Test
    void markRunning_onlyOneThreadWins_whenRacingConcurrently() throws InterruptedException {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        job.setStatus(JobStatus.QUEUED);

        // Each findById call returns a fresh copy of the job so threads see the same initial state
        when(jobRepository.findById(jobId)).thenAnswer(inv -> Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        int threadCount = 10;
        CountDownLatch startGate = new CountDownLatch(1); // holds all threads at the starting line
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await(); // all threads wait here until released together
                    jobStateManager.markRunning(jobId);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failureCount.incrementAndGet(); // expected for the 9 losers
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // release all 10 threads simultaneously
        doneLatch.await();     // wait for all to finish
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(9);
        assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(job.getStartedAt()).isNotNull();
    }

    @Test
    void markQueued_throwsIllegalStateException_whenJobNotPending() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        job.setStatus(JobStatus.RUNNING);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jobStateManager.markQueued(jobId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void markDone_setsResultAndCompletedAt() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        job.setStatus(JobStatus.RUNNING);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        jobStateManager.markDone(jobId, "Email sent");

        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(job.getResult()).isEqualTo("Email sent");
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void markFailed_setsErrorMessageAndCompletedAt() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        job.setStatus(JobStatus.RUNNING);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        jobStateManager.markFailed(jobId, "SMTP timeout");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("SMTP timeout");
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void markCancelled_succeedsForPendingAndQueued() {
        for (JobStatus cancellable : new JobStatus[]{JobStatus.PENDING, JobStatus.QUEUED}) {
            UUID jobId = UUID.randomUUID();
            Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
            job.setStatus(cancellable);
            when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

            jobStateManager.markCancelled(jobId);

            assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
        }
    }
}
