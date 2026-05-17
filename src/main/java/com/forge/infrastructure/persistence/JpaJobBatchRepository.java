package com.forge.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forge.domain.model.JobBatch;
import com.forge.domain.repository.IJobBatchRepository;

public interface JpaJobBatchRepository extends JpaRepository<JobBatch, UUID>, IJobBatchRepository{
    
}
