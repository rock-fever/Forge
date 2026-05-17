package com.forge.api.dto;

import com.forge.domain.model.Client;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ClientResponse {

    private final UUID id;
    private final String name;
    private final String apiKey;
    private final int maxConcurrentJobs;
    private final OffsetDateTime createdAt;

    private ClientResponse(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.apiKey = builder.apiKey;
        this.maxConcurrentJobs = builder.maxConcurrentJobs;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getApiKey() { return apiKey; }
    public int getMaxConcurrentJobs() { return maxConcurrentJobs; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public static ClientResponse from(Client client) {
        return new Builder()
                .id(client.getId())
                .name(client.getName())
                .apiKey(client.getApiKey())
                .maxConcurrentJobs(client.getMaxConcurrentJobs())
                .createdAt(client.getCreatedAt())
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String name;
        private String apiKey;
        private int maxConcurrentJobs;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder maxConcurrentJobs(int max) { this.maxConcurrentJobs = max; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ClientResponse build() { return new ClientResponse(this); }
    }
}
