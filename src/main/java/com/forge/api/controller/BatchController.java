package com.forge.api.controller;

import com.forge.api.dto.BatchResponse;
import com.forge.api.dto.CreateBatchRequest;
import com.forge.application.service.BatchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse create(@RequestBody CreateBatchRequest request) {
        return BatchResponse.from(
                batchService.createBatch(request.getClientId(), request.getName(), request.getJobs())
        );
    }

    // Blocks until all jobs in the batch complete or timeout expires, then returns current state.
    @GetMapping("/{id}")
    public BatchResponse get(@PathVariable UUID id,
                             @RequestParam(defaultValue = "30") int timeoutSeconds) {
        batchService.awaitCompletion(id, timeoutSeconds);
        return BatchResponse.from(batchService.getBatch(id));
    }
}
