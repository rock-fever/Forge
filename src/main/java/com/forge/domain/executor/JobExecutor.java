package com.forge.domain.executor;

import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobType;

public interface JobExecutor {
    JobType supportedType();
    JobResult execute(Job job) throws InterruptedException;
}
