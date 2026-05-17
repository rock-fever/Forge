package com.forge.infrastructure.executor;

import com.forge.domain.executor.JobExecutor;
import com.forge.domain.executor.JobResult;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobType;
import org.springframework.stereotype.Component;

@Component
public class SlowJobExecutor implements JobExecutor {

    @Override
    public JobType supportedType() {
        return JobType.SLOW;
    }

    @Override
    public JobResult execute(Job job) throws InterruptedException {
        Thread.sleep(2000);
        return JobResult.success("Slow job completed");
    }
}
