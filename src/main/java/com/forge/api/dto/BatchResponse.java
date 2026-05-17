package com.forge.api.dto;

import com.forge.domain.model.JobBatch;
import com.forge.domain.model.enums.JobStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BatchResponse {

    private final UUID id;
    private final UUID clientId;
    private final String name;
    private final int totalJobs;
    private final int completedJobs;
    private final int failedJobs;
    private final JobStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime completedAt;

    private BatchResponse(Builder builder) {
        this.id = builder.id;
        this.clientId = builder.clientId;
        this.name = builder.name;
        this.totalJobs = builder.totalJobs;
        this.completedJobs = builder.completedJobs;
        this.failedJobs = builder.failedJobs;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.completedAt = builder.completedAt;
    }

    public UUID getId() { return id; }
    public UUID getClientId() { return clientId; }
    public String getName() { return name; }
    public int getTotalJobs() { return totalJobs; }
    public int getCompletedJobs() { return completedJobs; }
    public int getFailedJobs() { return failedJobs; }
    public JobStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }

    public static BatchResponse from(JobBatch batch) {
        return new Builder()
                .id(batch.getId())
                .clientId(batch.getClient().getId())
                .name(batch.getName())
                .totalJobs(batch.getTotalJobs())
                .completedJobs(batch.getCompletedJobs())
                .failedJobs(batch.getFailedJobs())
                .status(batch.getStatus())
                .createdAt(batch.getCreatedAt())
                .completedAt(batch.getCompletedAt())
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID clientId;
        private String name;
        private int totalJobs;
        private int completedJobs;
        private int failedJobs;
        private JobStatus status;
        private OffsetDateTime createdAt;
        private OffsetDateTime completedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder clientId(UUID clientId) { this.clientId = clientId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalJobs(int totalJobs) { this.totalJobs = totalJobs; return this; }
        public Builder completedJobs(int completedJobs) { this.completedJobs = completedJobs; return this; }
        public Builder failedJobs(int failedJobs) { this.failedJobs = failedJobs; return this; }
        public Builder status(JobStatus status) { this.status = status; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder completedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; return this; }

        public BatchResponse build() { return new BatchResponse(this); }
    }
}
