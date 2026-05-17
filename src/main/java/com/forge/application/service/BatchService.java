package com.forge.application.service;

import com.forge.api.dto.CreateBatchRequest;
import com.forge.domain.model.Client;
import com.forge.domain.model.Job;
import com.forge.domain.model.JobBatch;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.repository.IClientRepository;
import com.forge.domain.repository.IJobBatchRepository;
import com.forge.domain.repository.IJobRepository;
import com.forge.shared.exception.ClientNotFoundException;
import com.forge.shared.exception.JobNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class BatchService {

    private final IJobBatchRepository jobBatchRepository;
    private final IJobRepository jobRepository;
    private final IClientRepository clientRepository;

    // One latch per active batch — removed after all jobs complete
    private final ConcurrentHashMap<UUID, CountDownLatch> latches = new ConcurrentHashMap<>();

    public BatchService(IJobBatchRepository jobBatchRepository, IClientRepository clientRepository,
            IJobRepository jobRepository) {
        this.jobBatchRepository = jobBatchRepository;
        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
    }

    public JobBatch createBatch(UUID clientId, String name, List<CreateBatchRequest.JobSpec> jobSpecs) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        JobBatch batch = JobBatch.builder()
                .client(client)
                .name(name != null ? name : UUID.randomUUID().toString())
                .totalJobs(jobSpecs.size())
                .build();

        JobBatch savedBatch = jobBatchRepository.save(batch);

        for (CreateBatchRequest.JobSpec spec : jobSpecs) {
            Job job = Job.builder()
                    .client(client)
                    .batchId(savedBatch.getId())
                    .type(spec.getType())
                    .payload(spec.getPayload())
                    .priority(spec.getPriority())
                    .maxRetries(spec.getMaxRetries())
                    .build();
            jobRepository.save(job);
        }

        latches.put(savedBatch.getId(), new CountDownLatch(jobSpecs.size()));

        return savedBatch;
    }

    @Transactional(readOnly = true)
    public JobBatch getBatch(UUID batchId) {
        return jobBatchRepository.findById(batchId)
                .orElseThrow(() -> new JobNotFoundException(batchId));
    }

    public void awaitCompletion(UUID batchId, int timeoutSeconds) {
        CountDownLatch latch = latches.get(batchId);
        if (latch != null) {
            try {
                latch.await(timeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void jobFinished(UUID batchId, JobStatus finalStatus) {
        JobBatch batch = jobBatchRepository.findById(batchId)
                .orElseThrow(() -> new JobNotFoundException(batchId));

        if (finalStatus == JobStatus.DONE) {
            batch.setCompletedJobs(batch.getCompletedJobs() + 1);
        } else {
            batch.setFailedJobs(batch.getFailedJobs() + 1);
        }

        boolean allFinished = (batch.getCompletedJobs() + batch.getFailedJobs()) == batch.getTotalJobs();
        if (allFinished) {
            batch.setStatus(JobStatus.DONE);
            batch.setCompletedAt(OffsetDateTime.now());
        }

        jobBatchRepository.save(batch);

        // Count down the latch only after the transaction commits.
        // Without this, a thread unblocked by the latch could read the batch from DB
        // before the updated counters and status are visible.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CountDownLatch latch = latches.get(batchId);
                if (latch != null) {
                    latch.countDown();
                    if (latch.getCount() == 0) {
                        latches.remove(batchId);
                    }
                }
            }
        });
    }
}
