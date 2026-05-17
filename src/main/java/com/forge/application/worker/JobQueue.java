package com.forge.application.worker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Component;

import com.forge.domain.model.Job;

@Component
public class JobQueue {
    private final BlockingQueue<Job> queue = new LinkedBlockingQueue<>(100); // max 100 jobs

    public void put(Job job) throws InterruptedException {
        queue.put(job);
    }

    public Job take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}
