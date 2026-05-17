package com.forge.application.worker;

import com.forge.application.metrics.ForgeMetrics;
import com.forge.domain.model.Client;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.model.enums.JobType;
import com.forge.domain.repository.IJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrySchedulerTest {

    @Mock IJobRepository jobRepository;
    @Mock JobStateManager jobStateManager;
    @Mock ScheduledExecutorService scheduler;
    @Mock ForgeMetrics forgeMetrics;

    RetryScheduler retryScheduler;

    Client client = Client.builder().name("Acme").apiKey("key-1").build();

    @BeforeEach
    void setUp() {
        retryScheduler = new RetryScheduler(jobRepository, jobStateManager, forgeMetrics, 1000L, 2.0, scheduler);
    }

    @Test
    void scheduleIfEligible_doesNothing_whenRetryCountEqualsMaxRetries() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").maxRetries(3).build();
        job.setStatus(JobStatus.FAILED);
        job.setRetryCount(3); // exhausted

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        retryScheduler.scheduleIfEligible(jobId);

        verifyNoInteractions(jobStateManager, scheduler);
    }

    @Test
    void scheduleIfEligible_markRetryingAndSchedulesPendingFlip_whenRetriesRemain() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").maxRetries(3).build();
        job.setStatus(JobStatus.FAILED);
        job.setRetryCount(0); // first failure

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        retryScheduler.scheduleIfEligible(jobId);

        // markRetrying must be called immediately with the computed retry time
        verify(jobStateManager).markRetrying(eq(jobId), any());
        verify(forgeMetrics).jobRetried();

        // the scheduler must be given a runnable to fire after 1000ms
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnableCaptor.capture(), eq(1000L), eq(TimeUnit.MILLISECONDS));

        // executing the scheduled runnable must flip the job to PENDING
        runnableCaptor.getValue().run();
        verify(jobStateManager).markPendingForRetry(jobId);
    }

    @Test
    void computeDelay_doublesWithEachRetry() {
        assertThat(retryScheduler.computeDelay(0)).isEqualTo(1000L);
        assertThat(retryScheduler.computeDelay(1)).isEqualTo(2000L);
        assertThat(retryScheduler.computeDelay(2)).isEqualTo(4000L);
    }
}
