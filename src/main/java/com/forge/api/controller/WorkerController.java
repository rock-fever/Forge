package com.forge.api.controller;

import com.forge.api.dto.WorkerStatusResponse;
import com.forge.application.metrics.ForgeMetrics;
import com.forge.application.worker.JobQueue;
import com.forge.application.worker.WorkerPool;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/workers")
public class WorkerController {

    private final WorkerPool workerPool;
    private final JobQueue jobQueue;
    private final ForgeMetrics forgeMetrics;

    public WorkerController(WorkerPool workerPool, JobQueue jobQueue, ForgeMetrics forgeMetrics) {
        this.workerPool   = workerPool;
        this.jobQueue     = jobQueue;
        this.forgeMetrics = forgeMetrics;
    }

    @GetMapping
    public ResponseEntity<WorkerStatusResponse> getStatus() {
        return ResponseEntity.ok(WorkerStatusResponse.builder()
                .poolSize(workerPool.getPoolSize())
                .activeWorkers(workerPool.getActiveCount())
                .queueDepth(jobQueue.size())
                .jobsSubmitted(forgeMetrics.getJobsSubmitted())
                .jobsCompleted(forgeMetrics.getJobsCompleted())
                .jobsFailed(forgeMetrics.getJobsFailed())
                .jobsRetried(forgeMetrics.getJobsRetried())
                .build());
    }
}
