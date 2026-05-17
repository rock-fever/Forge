package com.forge.application.service;

import com.forge.application.metrics.ForgeMetrics;
import com.forge.domain.model.Client;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.model.enums.JobType;
import com.forge.domain.repository.IClientRepository;
import com.forge.domain.repository.IJobRepository;
import com.forge.shared.exception.ClientNotFoundException;
import com.forge.shared.exception.CyclicDependencyException;
import com.forge.shared.exception.JobNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class JobService {

    private final IJobRepository jobRepository;
    private final IClientRepository clientRepository;
    private final ForgeMetrics forgeMetrics;

    public JobService(IJobRepository jobRepository, IClientRepository clientRepository,
                      ForgeMetrics forgeMetrics) {
        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
        this.forgeMetrics = forgeMetrics;
    }

    public Job submit(UUID clientId, JobType type, String payload, int priority, int maxRetries,
                      List<UUID> dependsOn) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        List<UUID> deps = dependsOn != null ? dependsOn : List.of();

        // Throws JobNotFoundException if any dep ID doesn't exist
        validateDepsExist(deps);

        // Throws CyclicDependencyException if adding these deps would create a cycle
        checkNoCycle(deps);

        // Job starts WAITING if it has unfinished deps, PENDING if it can run immediately
        boolean hasUnfinishedDeps = deps.stream()
                .map(id -> jobRepository.findById(id).orElseThrow())
                .anyMatch(dep -> dep.getStatus() != JobStatus.DONE);

        Job job = Job.builder()
                .client(client)
                .type(type)
                .payload(payload)
                .priority(priority)
                .maxRetries(maxRetries)
                .build();

        if (hasUnfinishedDeps) {
            job.setStatus(JobStatus.WAITING);
        }

        job = jobRepository.save(job);

        for (UUID depId : deps) {
            jobRepository.saveDependency(job.getId(), depId);
        }

        forgeMetrics.jobSubmitted();
        return job;
    }

    @Transactional(readOnly = true)
    public Job getStatus(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    private void validateDepsExist(List<UUID> depIds) {
        for (UUID depId : depIds) {
            jobRepository.findById(depId)
                    .orElseThrow(() -> new JobNotFoundException(depId));
        }
    }

    // Standard DFS cycle detection on the existing dependency graph.
    // A cycle exists if, starting from any provided dep, we can reach another provided dep
    // via transitive dependencies — meaning those deps are already in a cycle.
    private void checkNoCycle(List<UUID> depIds) {
        Set<UUID> depSet = new HashSet<>(depIds);
        Set<UUID> globalVisited = new HashSet<>();

        for (UUID startId : depIds) {
            if (globalVisited.contains(startId)) continue;
            Set<UUID> inStack = new HashSet<>();
            dfsCycleCheck(startId, depSet, globalVisited, inStack);
        }
    }

    private void dfsCycleCheck(UUID current, Set<UUID> depSet,
                                Set<UUID> globalVisited, Set<UUID> inStack) {
        globalVisited.add(current);
        inStack.add(current);

        for (Job transitiveDep : jobRepository.findDependenciesOf(current)) {
            UUID nextId = transitiveDep.getId();
            if (inStack.contains(nextId)) {
                throw new CyclicDependencyException(nextId);
            }
            if (!globalVisited.contains(nextId)) {
                dfsCycleCheck(nextId, depSet, globalVisited, inStack);
            }
        }

        inStack.remove(current);
    }
}
