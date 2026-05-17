package com.forge.infrastructure.config;

import com.forge.domain.executor.JobExecutor;
import com.forge.domain.executor.JobExecutorRegistry;
import com.forge.domain.model.enums.JobType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class WorkerConfig {

    @Bean
    public JobExecutorRegistry jobExecutorRegistry(List<JobExecutor> executors) {
        Map<JobType, JobExecutor> map = executors.stream()
                .collect(Collectors.toMap(JobExecutor::supportedType, e -> e));
        return new JobExecutorRegistry(map);
    }
}
