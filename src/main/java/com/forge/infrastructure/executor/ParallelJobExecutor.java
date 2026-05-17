package com.forge.infrastructure.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.domain.executor.JobExecutor;
import com.forge.domain.executor.JobResult;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Component
public class ParallelJobExecutor implements JobExecutor {

    private static final int THRESHOLD = 10;

    private final ObjectMapper objectMapper;

    public ParallelJobExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType supportedType() {
        return JobType.PARALLEL;
    }

    @Override
    public JobResult execute(Job job) throws InterruptedException {
        List<String> items;
        try {
            items = objectMapper.readValue(job.getPayload(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return JobResult.failure("Invalid payload: " + e.getMessage());
        }

        List<String> results = ForkJoinPool.commonPool().invoke(new PayloadSplitTask(items));
        return JobResult.success("Processed " + results.size() + " items: " + results);
    }

    // Static so it doesn't hold a reference to the enclosing executor instance.
    static class PayloadSplitTask extends RecursiveTask<List<String>> {

        private final List<String> items;

        PayloadSplitTask(List<String> items) {
            this.items = items;
        }

        @Override
        protected List<String> compute() {
            if (items.size() <= THRESHOLD) {
                return processDirectly(items);
            }

            int mid = items.size() / 2;
            PayloadSplitTask left  = new PayloadSplitTask(items.subList(0, mid));
            PayloadSplitTask right = new PayloadSplitTask(items.subList(mid, items.size()));

            left.fork();                        // push left onto the pool asynchronously
            List<String> rightResult = right.compute();  // process right on this thread
            List<String> leftResult  = left.join();      // wait for left to finish

            List<String> merged = new ArrayList<>(leftResult);
            merged.addAll(rightResult);
            return merged;
        }

        private List<String> processDirectly(List<String> chunk) {
            // Simulate per-chunk work; each chunk runs on a ForkJoin worker thread.
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return chunk.stream()
                    .map(item -> "processed:" + item)
                    .collect(Collectors.toList());
        }
    }
}
