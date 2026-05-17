package com.forge.infrastructure.executor;

import com.forge.domain.executor.JobExecutor;
import com.forge.domain.executor.JobResult;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobType;
import org.springframework.stereotype.Component;

@Component
public class EmailJobExecutor implements JobExecutor {

    @Override
    public JobType supportedType() {
        return JobType.EMAIL;
    }

    @Override
    public JobResult execute(Job job) throws InterruptedException {
        Thread.sleep(100);
        return JobResult.success("Email sent");
    }
}
