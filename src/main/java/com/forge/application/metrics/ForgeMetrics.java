package com.forge.application.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ForgeMetrics {

    private final AtomicLong jobsSubmitted = new AtomicLong();
    private final AtomicLong jobsCompleted = new AtomicLong();
    private final AtomicLong jobsFailed    = new AtomicLong();
    private final AtomicLong jobsRetried   = new AtomicLong();

    public void jobSubmitted() { jobsSubmitted.incrementAndGet(); }
    public void jobCompleted() { jobsCompleted.incrementAndGet(); }
    public void jobFailed()    { jobsFailed.incrementAndGet(); }
    public void jobRetried()   { jobsRetried.incrementAndGet(); }

    public long getJobsSubmitted() { return jobsSubmitted.get(); }
    public long getJobsCompleted() { return jobsCompleted.get(); }
    public long getJobsFailed()    { return jobsFailed.get(); }
    public long getJobsRetried()   { return jobsRetried.get(); }
}
