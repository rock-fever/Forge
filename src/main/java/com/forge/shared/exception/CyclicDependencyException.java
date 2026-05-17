package com.forge.shared.exception;

import java.util.UUID;

public class CyclicDependencyException extends RuntimeException {
    public CyclicDependencyException(UUID jobId) {
        super("Cyclic dependency detected involving job " + jobId);
    }
}
