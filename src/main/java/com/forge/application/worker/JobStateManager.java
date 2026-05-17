package com.forge.application.worker;

import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.repository.IJobRepository;
import com.forge.shared.exception.JobNotFoundException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class JobStateManager {

    private final IJobRepository jobRepository;
    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public JobStateManager(IJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // PENDING → QUEUED  (dispatcher claims the job)
    public void markQueued(UUID jobId) {
        transition(jobId, JobStatus.PENDING, job -> {
            job.setStatus(JobStatus.QUEUED);
        });
    }

    // QUEUED → RUNNING  (worker starts execution)
    public void markRunning(UUID jobId) {
        transition(jobId, JobStatus.QUEUED, job -> {
            job.setStatus(JobStatus.RUNNING);
            job.setStartedAt(OffsetDateTime.now());
        });
    }

    // RUNNING → DONE  (executor returned success)
    public void markDone(UUID jobId, String result) {
        transition(jobId, JobStatus.RUNNING, job -> {
            job.setStatus(JobStatus.DONE);
            job.setResult(result);
            job.setCompletedAt(OffsetDateTime.now());
        });
    }

    // RUNNING → FAILED  (executor returned failure or threw)
    public void markFailed(UUID jobId, String errorMessage) {
        transition(jobId, JobStatus.RUNNING, job -> {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(OffsetDateTime.now());
        });
    }

    // PENDING | QUEUED → CANCELLED  (user cancels before execution)
    public void markCancelled(UUID jobId) {
        ReentrantLock lock = locks.computeIfAbsent(jobId, id -> new ReentrantLock());
        lock.lock();
        try {
            Job job = load(jobId);
            if (job.getStatus() != JobStatus.PENDING && job.getStatus() != JobStatus.QUEUED) {
                throw new IllegalStateException(
                    "Cannot cancel job " + jobId + " in status " + job.getStatus());
            }
            job.setStatus(JobStatus.CANCELLED);
            jobRepository.save(job);
        } finally {
            lock.unlock();
        }
    }

    // FAILED → RETRYING  (retry scheduler claims the job; increments attempt count)
    public void markRetrying(UUID jobId, OffsetDateTime scheduledAfter) {
        transition(jobId, JobStatus.FAILED, job -> {
            job.setStatus(JobStatus.RETRYING);
            job.setRetryCount(job.getRetryCount() + 1);
            job.setScheduledAfter(scheduledAfter);
        });
    }

    // RETRYING → PENDING  (delay elapsed; dispatcher can pick it up again)
    public void markPendingForRetry(UUID jobId) {
        transition(jobId, JobStatus.RETRYING, job -> {
            job.setStatus(JobStatus.PENDING);
            job.setScheduledAfter(null);
            job.setErrorMessage(null);
        });
    }

    // WAITING → PENDING  (all dependencies are now DONE — chain executor unlocks this job)
    public void markPendingFromWaiting(UUID jobId) {
        transition(jobId, JobStatus.WAITING, job -> {
            job.setStatus(JobStatus.PENDING);
        });
    }

    // WAITING → FAILED  (a dependency failed — cascade failure down the chain)
    public void markFailedDueToDependency(UUID jobId, String errorMessage) {
        transition(jobId, JobStatus.WAITING, job -> {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(OffsetDateTime.now());
        });
    }

    private void transition(UUID jobId, JobStatus expectedFrom, java.util.function.Consumer<Job> update) {
        ReentrantLock lock = locks.computeIfAbsent(jobId, id -> new ReentrantLock());
        lock.lock();
        try {
            Job job = load(jobId);
            if (job.getStatus() != expectedFrom) {
                throw new IllegalStateException(
                    "Job " + jobId + ": expected " + expectedFrom + " but was " + job.getStatus());
            }
            update.accept(job);
            jobRepository.save(job);
        } finally {
            lock.unlock();
        }
    }

    private Job load(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }
}
