package com.forge.shared.context;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JobContextTest {

    @Test
    void set_and_get_returnSameValue() {
        UUID id = UUID.randomUUID();
        JobContext.set(id);
        assertThat(JobContext.get()).isEqualTo(id);
        JobContext.clear();
    }

    @Test
    void clear_removesValue() {
        JobContext.set(UUID.randomUUID());
        JobContext.clear();
        assertThat(JobContext.get()).isNull();
    }

    @Test
    void threadLocal_isolatesValuesBetweenThreads() throws InterruptedException {
        UUID mainId = UUID.randomUUID();
        JobContext.set(mainId);

        AtomicReference<UUID> otherThreadValue = new AtomicReference<>();
        Thread other = new Thread(() -> otherThreadValue.set(JobContext.get()));
        other.start();
        other.join();

        // other thread must see null — it never called set()
        assertThat(otherThreadValue.get()).isNull();
        // main thread still holds its value
        assertThat(JobContext.get()).isEqualTo(mainId);

        JobContext.clear();
    }
}
