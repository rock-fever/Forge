package com.forge.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.repository.IJobRepository;

public interface JpaJobRepository extends JpaRepository<Job, UUID>, IJobRepository {

    // findById, save, deleteById — inherited from JpaRepository, no declaration needed

    // Spring Data derives these from method name (client.id traversal handled automatically)
    List<Job> findByStatus(JobStatus status);

    List<Job> findByClientIdAndStatus(UUID clientId, JobStatus status);

    Page<Job> findByClientId(UUID clientId, Pageable pageable);

    Page<Job> findByClientIdAndStatus(UUID clientId, JobStatus status, Pageable pageable);

    List<Job> findByBatchId(UUID batchId);

    long countByClientIdAndStatus(UUID clientId, JobStatus status);

    // @Query needed: custom filter + LIMIT cannot be expressed by method name
    @Query(value = """
            SELECT * FROM jobs
            WHERE status = 'PENDING'
              AND (scheduled_after IS NULL OR scheduled_after <= :now)
            ORDER BY priority DESC, created_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Job> findPendingJobsReadyToRun(@Param("now") Instant now, @Param("limit") int limit);

    // Native SQL needed: job_dependencies is not a mapped JPA entity
    @Query(value = """
            SELECT j.* FROM jobs j
            JOIN job_dependencies jd ON j.id = jd.depends_on_job_id
            WHERE jd.job_id = :jobId
            """, nativeQuery = true)
    List<Job> findDependenciesOf(@Param("jobId") UUID jobId);

    @Query(value = """
            SELECT j.* FROM jobs j
            JOIN job_dependencies jd ON j.id = jd.job_id
            WHERE jd.depends_on_job_id = :jobId
            """, nativeQuery = true)
    List<Job> findDependentsOf(@Param("jobId") UUID jobId);
    @Modifying
    @Query(value = "INSERT INTO job_dependencies (job_id, depends_on_job_id) VALUES (:jobId, :dependsOnJobId)",
           nativeQuery = true)
    void saveDependency(@Param("jobId") UUID jobId, @Param("dependsOnJobId") UUID dependsOnJobId);

    @Modifying
    @Query(value = "UPDATE jobs SET status = 'PENDING' WHERE status = 'QUEUED'", nativeQuery = true)
    int resetQueuedToPending();
}
