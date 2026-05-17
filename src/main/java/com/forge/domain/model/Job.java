package com.forge.domain.model;

import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.model.enums.JobType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "batch_id")
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(nullable = false)
    private int priority = 0;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    private String result;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "scheduled_after")
    private OffsetDateTime scheduledAfter;

    protected Job() {}

    private Job(Builder builder) {
        this.client = builder.client;
        this.batchId = builder.batchId;
        this.type = builder.type;
        this.payload = builder.payload;
        this.priority = builder.priority;
        this.maxRetries = builder.maxRetries;
        this.scheduledAfter = builder.scheduledAfter;
    }

    public UUID getId() { return id; }
    public Client getClient() { return client; }
    public UUID getBatchId() { return batchId; }
    public JobType getType() { return type; }
    public String getPayload() { return payload; }
    public JobStatus getStatus() { return status; }
    public int getPriority() { return priority; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public String getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public OffsetDateTime getScheduledAfter() { return scheduledAfter; }

    public void setStatus(JobStatus status) { this.status = status; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public void setResult(String result) { this.result = result; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public void setScheduledAfter(OffsetDateTime scheduledAfter) { this.scheduledAfter = scheduledAfter; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Client client;
        private UUID batchId;
        private JobType type;
        private String payload;
        private int priority = 0;
        private int maxRetries = 3;
        private OffsetDateTime scheduledAfter;

        public Builder client(Client client) { this.client = client; return this; }
        public Builder batchId(UUID batchId) { this.batchId = batchId; return this; }
        public Builder type(JobType type) { this.type = type; return this; }
        public Builder payload(String payload) { this.payload = payload; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder scheduledAfter(OffsetDateTime scheduledAfter) { this.scheduledAfter = scheduledAfter; return this; }

        public Job build() { return new Job(this); }
    }
}
