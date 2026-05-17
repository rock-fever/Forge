package com.forge.api.dto;

import com.forge.domain.model.enums.JobType;

import java.util.List;
import java.util.UUID;

public class CreateJobRequest {

    private final UUID clientId;
    private final JobType type;
    private final String payload;
    private final int priority;
    private final int maxRetries;
    private final List<UUID> dependsOn;

    private CreateJobRequest(Builder builder) {
        this.clientId = builder.clientId;
        this.type = builder.type;
        this.payload = builder.payload;
        this.priority = builder.priority;
        this.maxRetries = builder.maxRetries;
        this.dependsOn = builder.dependsOn;
    }

    public UUID getClientId() { return clientId; }
    public JobType getType() { return type; }
    public String getPayload() { return payload; }
    public int getPriority() { return priority; }
    public int getMaxRetries() { return maxRetries; }
    public List<UUID> getDependsOn() { return dependsOn; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID clientId;
        private JobType type;
        private String payload;
        private int priority = 0;
        private int maxRetries = 3;
        private List<UUID> dependsOn;

        public Builder clientId(UUID clientId) { this.clientId = clientId; return this; }
        public Builder type(JobType type) { this.type = type; return this; }
        public Builder payload(String payload) { this.payload = payload; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder dependsOn(List<UUID> dependsOn) {this.dependsOn = dependsOn; return this;}

        public CreateJobRequest build() { return new CreateJobRequest(this); }
    }
}
