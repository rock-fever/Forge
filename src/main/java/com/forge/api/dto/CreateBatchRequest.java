package com.forge.api.dto;

import com.forge.domain.model.enums.JobType;

import java.util.List;
import java.util.UUID;

public class CreateBatchRequest {

    private UUID clientId;
    private String name;
    private List<JobSpec> jobs;

    public UUID getClientId() { return clientId; }
    public String getName() { return name; }
    public List<JobSpec> getJobs() { return jobs; }

    public static class JobSpec {
        private JobType type;
        private String payload;
        private int priority = 0;
        private int maxRetries = 3;

        public JobSpec() {}

        public JobSpec(JobType type, String payload, int priority, int maxRetries) {
            this.type = type;
            this.payload = payload;
            this.priority = priority;
            this.maxRetries = maxRetries;
        }

        public JobType getType() { return type; }
        public String getPayload() { return payload; }
        public int getPriority() { return priority; }
        public int getMaxRetries() { return maxRetries; }
    }
}
