package com.forge.application.worker;

import com.forge.domain.model.Job;

import com.forge.domain.repository.IJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

/**
 * Producer in the producer-consumer pattern.
 *
 * Runs on a fixed schedule, queries the DB for PENDING jobs that are ready
 * to run, and puts them onto the JobQueue. Worker threads on the other side
 * of the queue pick them up and execute them.
 *
 * The @Scheduled method runs on Spring's scheduler thread — it must never
 * block for long. If the queue is full, put() will block this thread until
 * space opens up (back-pressure).
 */
@Component
public class JobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);
    private static final int DISPATCH_BATCH_SIZE = 20;

    private final IJobRepository jobRepository;
    private final JobQueue jobQueue;
    private final JobStateManager jobStateManager;

    public JobDispatcher(IJobRepository jobRepository, JobQueue jobQueue, JobStateManager jobStateManager) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
        this.jobStateManager = jobStateManager;
    }

    @Scheduled(fixedDelayString = "${forge.dispatcher.interval-ms:1000}")
    @Transactional
    public void dispatch() {
        List<Job> pending = jobRepository.findPendingJobsReadyToRun(Instant.now(), DISPATCH_BATCH_SIZE);

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Dispatching {} pending jobs", pending.size());

        for (Job job : pending) {
            // Force-initialize lazy client proxy while the Hibernate session is still open.
            // JobWorker runs on a different thread with no session — without this,
            // job.getClient().getMaxConcurrentJobs() throws LazyInitializationException.
            job.getClient().getMaxConcurrentJobs();

            jobStateManager.markQueued(job.getId());
        }

        // Enqueue AFTER the transaction commits so markQueued is visible to workers.
        // Without this, a worker can read the job as PENDING (pre-commit) and fail.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Job job : pending) {
                    try {
                        jobQueue.put(job);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Dispatcher interrupted while enqueuing job {}", job.getId());
                        return;
                    }
                }
            }
        });
    }
}
