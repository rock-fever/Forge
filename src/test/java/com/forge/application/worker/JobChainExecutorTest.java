package com.forge.application.worker;

import com.forge.domain.model.Client;
import com.forge.domain.model.Job;
import com.forge.domain.model.enums.JobStatus;

import com.forge.domain.repository.IJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobChainExecutorTest {

    @Mock IJobRepository jobRepository;
    @Mock JobStateManager jobStateManager;

    JobChainExecutor jobChainExecutor;

    Client client = Client.builder().name("Acme").apiKey("key-1").build();

    @BeforeEach
    void setUp() {
        jobChainExecutor = new JobChainExecutor(jobRepository, jobStateManager);
    }

    @Test
    void onJobCompleted_unlocksDependentJob_whenAllDepsAreDone() {
        UUID completedJobId = UUID.randomUUID();

        // dependent job is WAITING
        Job dependent = jobWithStatus(JobStatus.WAITING);

        // the completed job is dependent's only dep, and it's DONE
        Job dep = jobWithStatus(JobStatus.DONE);

        when(jobRepository.findDependentsOf(completedJobId)).thenReturn(List.of(dependent));
        when(jobRepository.findDependenciesOf(dependent.getId())).thenReturn(List.of(dep));

        jobChainExecutor.onJobCompleted(completedJobId, JobStatus.DONE);

        verify(jobStateManager).markPendingFromWaiting(dependent.getId());
        verify(jobStateManager, never()).markFailedDueToDependency(any(), any());
    }

    @Test
    void onJobCompleted_cascadesFailure_whenAnyDepFailed() {
        UUID completedJobId = UUID.randomUUID();

        Job dependent = jobWithStatus(JobStatus.WAITING);

        Job failedDep = jobWithStatus(JobStatus.FAILED);
        Job doneDep   = jobWithStatus(JobStatus.DONE);

        when(jobRepository.findDependentsOf(completedJobId)).thenReturn(List.of(dependent));
        when(jobRepository.findDependenciesOf(dependent.getId())).thenReturn(List.of(doneDep, failedDep));

        jobChainExecutor.onJobCompleted(completedJobId, JobStatus.FAILED);

        verify(jobStateManager).markFailedDueToDependency(eq(dependent.getId()), anyString());
        verify(jobStateManager, never()).markPendingFromWaiting(any());
    }

    @Test
    void onJobCompleted_doesNothing_whenDependentStillHasUnfinishedDeps() {
        UUID completedJobId = UUID.randomUUID();

        Job dependent = jobWithStatus(JobStatus.WAITING);

        Job doneDep    = jobWithStatus(JobStatus.DONE);
        Job pendingDep = jobWithStatus(JobStatus.PENDING); // still running

        when(jobRepository.findDependentsOf(completedJobId)).thenReturn(List.of(dependent));
        when(jobRepository.findDependenciesOf(dependent.getId())).thenReturn(List.of(doneDep, pendingDep));

        jobChainExecutor.onJobCompleted(completedJobId, JobStatus.DONE);

        verify(jobStateManager, never()).markPendingFromWaiting(any());
        verify(jobStateManager, never()).markFailedDueToDependency(any(), any());
    }

    @Test
    void onJobCompleted_skipsNonWaitingDependents() {
        UUID completedJobId = UUID.randomUUID();

        // dependent is already RUNNING (was unlocked earlier) — should be skipped
        Job dependent = jobWithStatus(JobStatus.RUNNING);

        when(jobRepository.findDependentsOf(completedJobId)).thenReturn(List.of(dependent));

        jobChainExecutor.onJobCompleted(completedJobId, JobStatus.DONE);

        verify(jobStateManager, never()).markPendingFromWaiting(any());
        verify(jobStateManager, never()).markFailedDueToDependency(any(), any());
        verify(jobRepository, never()).findDependenciesOf(any());
    }

    @Test
    void onJobCompleted_doesNothing_whenNoDependentsExist() {
        UUID completedJobId = UUID.randomUUID();
        when(jobRepository.findDependentsOf(completedJobId)).thenReturn(List.of());

        jobChainExecutor.onJobCompleted(completedJobId, JobStatus.DONE);

        verifyNoInteractions(jobStateManager);
    }

    private Job jobWithStatus(JobStatus status) {
        Job job = mock(Job.class);
        lenient().when(job.getId()).thenReturn(UUID.randomUUID());
        when(job.getStatus()).thenReturn(status);
        return job;
    }
}
