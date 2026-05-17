package com.forge.application.worker;

import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;
import com.forge.domain.repository.IJobRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves job chains after each job completes.
 *
 * When Job A finishes, this executor finds all WAITING jobs that depended on A.
 * For each one it checks every dependency:
 *   - any dep FAILED  → cascade: mark the waiting job FAILED too
 *   - all deps DONE   → unlock: flip the waiting job to PENDING so the dispatcher picks it up
 *   - otherwise       → still waiting for other deps, do nothing
 */
@Component
public class JobChainExecutor {

    private final IJobRepository jobRepository;
    private final JobStateManager jobStateManager;

    public JobChainExecutor(IJobRepository jobRepository, JobStateManager jobStateManager) {
        this.jobRepository = jobRepository;
        this.jobStateManager = jobStateManager;
    }

    public void onJobCompleted(UUID completedJobId, JobStatus finalStatus) {
        List<Job> dependents = jobRepository.findDependentsOf(completedJobId);

        for (Job dependent : dependents) {
            if (dependent.getStatus() != JobStatus.WAITING) continue;

            List<Job> allDeps = jobRepository.findDependenciesOf(dependent.getId());

            boolean anyDepFailed = allDeps.stream()
                    .anyMatch(d -> d.getStatus() == JobStatus.FAILED);

            boolean allDepsDone = allDeps.stream()
                    .allMatch(d -> d.getStatus() == JobStatus.DONE);

            if (anyDepFailed) {
                Optional<Job> failedDep = allDeps.stream()
                        .filter(d -> d.getStatus() == JobStatus.FAILED)
                        .findFirst();
                String reason = "Dependency job " + failedDep.map(d -> d.getId().toString())
                        .orElse("unknown") + " failed";
                jobStateManager.markFailedDueToDependency(dependent.getId(), reason);
            } else if (allDepsDone) {
                jobStateManager.markPendingFromWaiting(dependent.getId());
            }
            // else: other deps still unfinished — leave as WAITING
        }
    }
}
