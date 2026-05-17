package com.forge.integration;

import com.forge.AbstractIntegrationTest;
import com.forge.application.service.BatchService;
import com.forge.application.service.ClientService;
import com.forge.application.service.JobService;
import com.forge.application.worker.StartupRecovery;
import com.forge.domain.model.Client;
import com.forge.domain.model.Job;
import com.forge.domain.model.JobBatch;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.model.enums.JobType;
import com.forge.domain.repository.IJobBatchRepository;
import com.forge.domain.repository.IJobRepository;
import com.forge.domain.executor.JobResult;
import com.forge.infrastructure.executor.EmailJobExecutor;
import com.forge.api.dto.CreateBatchRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class JobLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired JobService jobService;
    @Autowired BatchService batchService;
    @Autowired ClientService clientService;
    @Autowired IJobRepository jobRepository;
    @Autowired IJobBatchRepository jobBatchRepository;
    @Autowired StartupRecovery startupRecovery;
    @Autowired JdbcTemplate jdbcTemplate;

    @SpyBean EmailJobExecutor emailJobExecutor;

    Client client;

    @BeforeEach
    void setUp() {
        // Let any in-flight worker operations from a previous test settle before wiping the DB.
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        jdbcTemplate.execute("TRUNCATE TABLE job_dependencies, jobs, job_batches, clients CASCADE");
        Mockito.reset(emailJobExecutor);
        client = clientService.register("Test Client", "key-" + UUID.randomUUID(), 5);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(emailJobExecutor);
    }

    // ── Test 1: full job lifecycle ──────────────────────────────────────────────

    @Test
    void fullLifecycle_emailJob_completesSuccessfully() throws InterruptedException {
        Job job = jobService.submit(client.getId(), JobType.EMAIL, "{}", 0, 0, List.of());

        Job done = awaitStatus(job.getId(), JobStatus.DONE, 10);

        assertThat(done.getResult()).isEqualTo("Email sent");
        assertThat(done.getStartedAt()).isNotNull();
        assertThat(done.getCompletedAt()).isNotNull();
    }

    // ── Test 2: batch ───────────────────────────────────────────────────────────

    @Test
    void batchLifecycle_threeJobs_allComplete() throws InterruptedException {
        List<CreateBatchRequest.JobSpec> specs = List.of(
                new CreateBatchRequest.JobSpec(JobType.EMAIL, "{}", 0, 0),
                new CreateBatchRequest.JobSpec(JobType.EMAIL, "{}", 0, 0),
                new CreateBatchRequest.JobSpec(JobType.EMAIL, "{}", 0, 0)
        );
        JobBatch batch = batchService.createBatch(client.getId(), "integration-batch", specs);

        JobBatch done = awaitBatchDone(batch.getId(), 15);

        assertThat(done.getCompletedJobs()).isEqualTo(3);
        assertThat(done.getFailedJobs()).isEqualTo(0);
        assertThat(done.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(done.getCompletedAt()).isNotNull();
    }

    // ── Test 3: retry ───────────────────────────────────────────────────────────

    @Test
    void retry_jobFailsTwiceAndSucceedsOnThirdAttempt() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        doAnswer(inv -> {
            int n = calls.incrementAndGet();
            return n <= 2 ? JobResult.failure("simulated failure " + n) : JobResult.success("ok on attempt " + n);
        }).when(emailJobExecutor).execute(any());

        // maxRetries=3 so the job will be retried up to 3 times
        Job job = jobService.submit(client.getId(), JobType.EMAIL, "{}", 0, 3, List.of());

        Job done = awaitStatus(job.getId(), JobStatus.DONE, 15);

        assertThat(done.getResult()).isEqualTo("ok on attempt 3");
        assertThat(done.getRetryCount()).isEqualTo(2);
    }

    // ── Test 4: startup recovery ────────────────────────────────────────────────

    @Test
    void startupRecovery_resetsOrphanedQueuedJobsToPending() {
        // Simulate a job that was claimed by the dispatcher (QUEUED) but never reached
        // the in-memory queue because the process crashed before enqueuing it.
        Job orphan = Job.builder().client(client).type(JobType.EMAIL).payload("{}").maxRetries(0).build();
        orphan.setStatus(JobStatus.QUEUED);
        orphan = jobRepository.save(orphan);
        UUID orphanId = orphan.getId();

        startupRecovery.recoverQueuedJobs();

        Job recovered = jobRepository.findById(orphanId).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private Job awaitStatus(UUID jobId, JobStatus expected, int timeoutSeconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Job job = jobRepository.findById(jobId).orElseThrow();
            if (job.getStatus() == expected) return job;
            Thread.sleep(200);
        }
        Job current = jobRepository.findById(jobId).orElseThrow();
        throw new AssertionError(
                "Job " + jobId + " did not reach " + expected + " within " + timeoutSeconds
                + "s — current status: " + current.getStatus());
    }

    private JobBatch awaitBatchDone(UUID batchId, int timeoutSeconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            JobBatch batch = jobBatchRepository.findById(batchId).orElseThrow();
            if (batch.getStatus() == JobStatus.DONE) return batch;
            Thread.sleep(200);
        }
        JobBatch current = jobBatchRepository.findById(batchId).orElseThrow();
        throw new AssertionError(
                "Batch " + batchId + " did not complete within " + timeoutSeconds
                + "s — completedJobs=" + current.getCompletedJobs()
                + ", failedJobs=" + current.getFailedJobs());
    }
}
