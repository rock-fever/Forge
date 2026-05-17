package com.forge.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    @Column(name = "max_concurrent_jobs", nullable = false)
    private int maxConcurrentJobs = 5;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Client() {}

    private Client(Builder builder) {
        this.name = builder.name;
        this.apiKey = builder.apiKey;
        this.maxConcurrentJobs = builder.maxConcurrentJobs;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getApiKey() { return apiKey; }
    public int getMaxConcurrentJobs() { return maxConcurrentJobs; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String name;
        private String apiKey;
        private int maxConcurrentJobs = 5;

        public Builder name(String name) { this.name = name; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder maxConcurrentJobs(int max) { this.maxConcurrentJobs = max; return this; }

        public Client build() { return new Client(this); }
    }
}
