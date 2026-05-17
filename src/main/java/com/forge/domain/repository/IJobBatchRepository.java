package com.forge.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.forge.domain.model.JobBatch;

public interface IJobBatchRepository {
    Optional<JobBatch> findById(UUID id);

    JobBatch save(JobBatch jobBatch);
}
