package com.forge.api.dto;

public class WorkerStatusResponse {

    private final int poolSize;
    private final int activeWorkers;
    private final int queueDepth;
    private final long jobsSubmitted;
    private final long jobsCompleted;
    private final long jobsFailed;
    private final long jobsRetried;

    private WorkerStatusResponse(Builder b) {
        this.poolSize       = b.poolSize;
        this.activeWorkers  = b.activeWorkers;
        this.queueDepth     = b.queueDepth;
        this.jobsSubmitted  = b.jobsSubmitted;
        this.jobsCompleted  = b.jobsCompleted;
        this.jobsFailed     = b.jobsFailed;
        this.jobsRetried    = b.jobsRetried;
    }

    public int getPoolSize()        { return poolSize; }
    public int getActiveWorkers()   { return activeWorkers; }
    public int getQueueDepth()      { return queueDepth; }
    public long getJobsSubmitted()  { return jobsSubmitted; }
    public long getJobsCompleted()  { return jobsCompleted; }
    public long getJobsFailed()     { return jobsFailed; }
    public long getJobsRetried()    { return jobsRetried; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int poolSize;
        private int activeWorkers;
        private int queueDepth;
        private long jobsSubmitted;
        private long jobsCompleted;
        private long jobsFailed;
        private long jobsRetried;

        public Builder poolSize(int v)        { this.poolSize = v;       return this; }
        public Builder activeWorkers(int v)   { this.activeWorkers = v;  return this; }
        public Builder queueDepth(int v)      { this.queueDepth = v;     return this; }
        public Builder jobsSubmitted(long v)  { this.jobsSubmitted = v;  return this; }
        public Builder jobsCompleted(long v)  { this.jobsCompleted = v;  return this; }
        public Builder jobsFailed(long v)     { this.jobsFailed = v;     return this; }
        public Builder jobsRetried(long v)    { this.jobsRetried = v;    return this; }

        public WorkerStatusResponse build() { return new WorkerStatusResponse(this); }
    }
}
