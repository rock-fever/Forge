package com.forge.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;

public interface IJobRepository {
    // Basic CRUD
    Optional<Job> findById(UUID id);

    Job save(Job job);

    void deleteById(UUID id);

    // Status queries (used by JobDispatcher, status polling)
    List<Job> findByStatus(JobStatus status);

    List<Job> findByClientIdAndStatus(UUID clientId, JobStatus status);

    // Dispatcher: picks up jobs ready to run (respects scheduled_after for retries)
    List<Job> findPendingJobsReadyToRun(Instant now, int limit);

    // Listing with filters (GET /jobs?clientId=&status=&page=&size=)
    Page<Job> findByClientId(UUID clientId, Pageable pageable);

    Page<Job> findByClientIdAndStatus(UUID clientId, JobStatus status, Pageable pageable);

    // Batch membership
    List<Job> findByBatchId(UUID batchId);

    // Dependency resolution (JobChainExecutor)
    List<Job> findDependenciesOf(UUID jobId); // jobs this job depends on

    List<Job> findDependentsOf(UUID jobId); // jobs waiting on this job

    void saveDependency(UUID jobId, UUID dependsOnJobId);

    // Startup recovery: reset orphaned QUEUED jobs left by a previous crash
    int resetQueuedToPending();

    // Metrics / stats
    long countByClientIdAndStatus(UUID clientId, JobStatus status);

}
