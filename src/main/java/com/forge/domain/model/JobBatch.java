package com.forge.domain.model;

import com.forge.domain.model.enums.JobStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_batches")
public class JobBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_jobs", nullable = false)
    private int totalJobs;

    @Column(name = "completed_jobs", nullable = false)
    private int completedJobs = 0;

    @Column(name = "failed_jobs", nullable = false)
    private int failedJobs = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected JobBatch() {}

    private JobBatch(Builder builder) {
        this.client = builder.client;
        this.name = builder.name;
        this.totalJobs = builder.totalJobs;
    }

    public UUID getId() { return id; }
    public Client getClient() { return client; }
    public String getName() { return name; }
    public int getTotalJobs() { return totalJobs; }
    public int getCompletedJobs() { return completedJobs; }
    public int getFailedJobs() { return failedJobs; }
    public JobStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }

    public void setCompletedJobs(int completedJobs) { this.completedJobs = completedJobs; }
    public void setFailedJobs(int failedJobs) { this.failedJobs = failedJobs; }
    public void setStatus(JobStatus status) { this.status = status; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Client client;
        private String name;
        private int totalJobs;

        public Builder client(Client client) { this.client = client; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalJobs(int totalJobs) { this.totalJobs = totalJobs; return this; }

        public JobBatch build() { return new JobBatch(this); }
    }
}
