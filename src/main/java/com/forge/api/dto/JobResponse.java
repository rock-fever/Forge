package com.forge.api.dto;

import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.model.enums.JobType;

import java.time.OffsetDateTime;
import java.util.UUID;

public class JobResponse {

    private final UUID id;
    private final UUID clientId;
    private final UUID batchId;
    private final JobType type;
    private final String payload;
    private final JobStatus status;
    private final int priority;
    private final int retryCount;
    private final String result;
    private final String errorMessage;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime completedAt;

    private JobResponse(Builder builder) {
        this.id = builder.id;
        this.clientId = builder.clientId;
        this.batchId = builder.batchId;
        this.type = builder.type;
        this.payload = builder.payload;
        this.status = builder.status;
        this.priority = builder.priority;
        this.retryCount = builder.retryCount;
        this.result = builder.result;
        this.errorMessage = builder.errorMessage;
        this.createdAt = builder.createdAt;
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
    }

    public UUID getId() { return id; }
    public UUID getClientId() { return clientId; }
    public UUID getBatchId() { return batchId; }
    public JobType getType() { return type; }
    public String getPayload() { return payload; }
    public JobStatus getStatus() { return status; }
    public int getPriority() { return priority; }
    public int getRetryCount() { return retryCount; }
    public String getResult() { return result; }
    public String getErrorMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }

    public static JobResponse from(Job job) {
        return new Builder()
                .id(job.getId())
                .clientId(job.getClient().getId())
                .batchId(job.getBatchId())
                .type(job.getType())
                .payload(job.getPayload())
                .status(job.getStatus())
                .priority(job.getPriority())
                .retryCount(job.getRetryCount())
                .result(job.getResult())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID clientId;
        private UUID batchId;
        private JobType type;
        private String payload;
        private JobStatus status;
        private int priority;
        private int retryCount;
        private String result;
        private String errorMessage;
        private OffsetDateTime createdAt;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder clientId(UUID clientId) { this.clientId = clientId; return this; }
        public Builder batchId(UUID batchId) { this.batchId = batchId; return this; }
        public Builder type(JobType type) { this.type = type; return this; }
        public Builder payload(String payload) { this.payload = payload; return this; }
        public Builder status(JobStatus status) { this.status = status; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
        public Builder result(String result) { this.result = result; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder startedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder completedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; return this; }

        public JobResponse build() { return new JobResponse(this); }
    }
}
