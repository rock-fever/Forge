package com.forge.shared.context;

import java.util.UUID;

public final class JobContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private JobContext() {}

    public static void set(UUID jobId) {
        CURRENT.set(jobId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    // Use remove() rather than set(null) to fully clear the ThreadLocal entry
    // and avoid memory leaks when threads are pooled and reused.
    public static void clear() {
        CURRENT.remove();
    }
}
