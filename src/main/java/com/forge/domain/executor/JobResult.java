package com.forge.domain.executor;

public record JobResult(String output, boolean success) {

    public static JobResult success(String output) {
        return new JobResult(output, true);
    }

    public static JobResult failure(String error) {
        return new JobResult(error, false);
    }
}
