package com.forge.application.worker;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Component;

@Component
public class ClientConcurrencyLimiter {
    private final ConcurrentHashMap<UUID, Semaphore> semaphores = new ConcurrentHashMap<>();

    public void acquire(UUID clientId, int maxConcurrentJobs) {
        try {
            semaphores.computeIfAbsent(clientId, id -> new Semaphore(maxConcurrentJobs))
                    .acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void release(UUID clientId) {
        Semaphore semaphore = semaphores.get(clientId);
        if (semaphore != null) {
            semaphore.release();
        }
    }
}
