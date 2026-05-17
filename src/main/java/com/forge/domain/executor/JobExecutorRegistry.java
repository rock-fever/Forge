package com.forge.domain.executor;

import com.forge.domain.model.enums.JobType;

import java.util.Map;

public class JobExecutorRegistry {

    private final Map<JobType, JobExecutor> registry;

    public JobExecutorRegistry(Map<JobType, JobExecutor> registry) {
        this.registry = Map.copyOf(registry);
    }

    public JobExecutor get(JobType type) {
        JobExecutor executor = registry.get(type);
        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for type: " + type);
        }
        return executor;
    }
}
