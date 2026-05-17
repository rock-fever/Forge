package com.forge.api.controller;

import com.forge.api.dto.CreateJobRequest;
import com.forge.api.dto.JobResponse;
import com.forge.application.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse submit(@RequestBody CreateJobRequest request) {
        return JobResponse.from(
                jobService.submit(
                        request.getClientId(),
                        request.getType(),
                        request.getPayload(),
                        request.getPriority(),
                        request.getMaxRetries(),
                        request.getDependsOn()
                )
        );
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return JobResponse.from(jobService.getStatus(id));
    }
}
