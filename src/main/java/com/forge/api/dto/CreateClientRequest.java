package com.forge.api.dto;

public class CreateClientRequest {

    private final String name;
    private final String apiKey;
    private final int maxConcurrentJobs;

    private CreateClientRequest(Builder builder) {
        this.name = builder.name;
        this.apiKey = builder.apiKey;
        this.maxConcurrentJobs = builder.maxConcurrentJobs;
    }

    public String getName() { return name; }
    public String getApiKey() { return apiKey; }
    public int getMaxConcurrentJobs() { return maxConcurrentJobs; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String name;
        private String apiKey;
        private int maxConcurrentJobs = 5;

        public Builder name(String name) { this.name = name; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder maxConcurrentJobs(int max) { this.maxConcurrentJobs = max; return this; }

        public CreateClientRequest build() { return new CreateClientRequest(this); }
    }
}
