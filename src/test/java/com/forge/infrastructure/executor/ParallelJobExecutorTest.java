package com.forge.infrastructure.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.domain.executor.JobResult;
import com.forge.domain.model.Client;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobType;
import com.forge.infrastructure.executor.ParallelJobExecutor.PayloadSplitTask;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ParallelJobExecutorTest {

    ObjectMapper objectMapper = new ObjectMapper();
    Client client = Client.builder().name("Acme").apiKey("key-1").build();

    @Test
    void payloadSplitTask_processesAllItems_withoutLoss() {
        List<String> items = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> "item" + i)
                .collect(Collectors.toList());

        List<String> results = new PayloadSplitTask(items).invoke();

        assertThat(results).hasSize(100);
        // every item must appear exactly once with the processed: prefix
        for (int i = 1; i <= 100; i++) {
            assertThat(results).contains("processed:item" + i);
        }
    }

    @Test
    void payloadSplitTask_handlesListSmallerThanThreshold() {
        List<String> items = List.of("a", "b", "c");

        List<String> results = new PayloadSplitTask(items).invoke();

        assertThat(results).containsExactlyInAnyOrder("processed:a", "processed:b", "processed:c");
    }

    @Test
    void execute_returnsSuccess_forValidJsonArrayPayload() throws InterruptedException {
        String payload = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> "\"item" + i + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        Job job = Job.builder().client(client).type(JobType.PARALLEL).payload(payload).build();

        JobResult result = new ParallelJobExecutor(objectMapper).execute(job);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Processed 20 items");
    }

    @Test
    void execute_returnsFailure_forInvalidJsonPayload() throws InterruptedException {
        Job job = Job.builder().client(client).type(JobType.PARALLEL).payload("not-json").build();

        JobResult result = new ParallelJobExecutor(objectMapper).execute(job);

        assertThat(result.success()).isFalse();
        assertThat(result.output()).startsWith("Invalid payload:");
    }
}
