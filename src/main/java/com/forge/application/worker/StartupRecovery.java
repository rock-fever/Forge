package com.forge.application.worker;

import com.forge.domain.repository.IJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(StartupRecovery.class);

    private final IJobRepository jobRepository;

    public StartupRecovery(IJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // Runs after the full application context is ready (after @PostConstruct, after workers start).
    // Resets any jobs stuck in QUEUED from a previous process crash — those jobs made it to the DB
    // but never reached the in-memory queue, so they would never be picked up without this reset.
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverQueuedJobs() {
        int recovered = jobRepository.resetQueuedToPending();
        if (recovered > 0) {
            log.warn("Recovered {} orphaned QUEUED jobs to PENDING after startup", recovered);
        }
    }
}
