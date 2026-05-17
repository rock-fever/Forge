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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock IJobRepository jobRepository;
    @Mock IClientRepository clientRepository;
    @Mock ForgeMetrics forgeMetrics;

    @InjectMocks
    JobService jobService;

    Client client = Client.builder().name("Acme").apiKey("key-1").build();

    @Test
    void submit_savesJobAsPending_whenNoDependencies() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = jobService.submit(clientId, JobType.EMAIL, "{}", 0, 3, List.of());

        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        verify(jobRepository, times(1)).save(any(Job.class));
        verify(jobRepository, never()).saveDependency(any(), any());
    }

    @Test
    void submit_savesJobAsWaiting_whenDepIsNotDone() {
        UUID clientId = UUID.randomUUID();
        UUID depId = UUID.randomUUID();
        Job dep = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        // dep is PENDING — not done yet
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(jobRepository.findById(depId)).thenReturn(Optional.of(dep));
        when(jobRepository.findDependenciesOf(depId)).thenReturn(List.of());
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = jobService.submit(clientId, JobType.EMAIL, "{}", 0, 3, List.of(depId));

        assertThat(result.getStatus()).isEqualTo(JobStatus.WAITING);
        verify(jobRepository).saveDependency(result.getId(), depId);
    }

    @Test
    void submit_savesJobAsPending_whenAllDepsAreDone() {
        UUID clientId = UUID.randomUUID();
        UUID depId = UUID.randomUUID();
        Job dep = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        dep.setStatus(JobStatus.DONE);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(jobRepository.findById(depId)).thenReturn(Optional.of(dep));
        when(jobRepository.findDependenciesOf(depId)).thenReturn(List.of());
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = jobService.submit(clientId, JobType.EMAIL, "{}", 0, 3, List.of(depId));

        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        verify(jobRepository).saveDependency(result.getId(), depId);
    }

    @Test
    void submit_throwsJobNotFoundException_whenDepDoesNotExist() {
        UUID clientId = UUID.randomUUID();
        UUID missingDepId = UUID.randomUUID();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(jobRepository.findById(missingDepId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                jobService.submit(clientId, JobType.EMAIL, "{}", 0, 3, List.of(missingDepId)))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void submit_throwsCyclicDependencyException_whenCycleExists() {
        UUID clientId = UUID.randomUUID();
        UUID jobA = UUID.randomUUID();
        UUID jobB = UUID.randomUUID();

        Job depA = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        Job depB = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(jobRepository.findById(jobA)).thenReturn(Optional.of(depA));
        when(jobRepository.findById(jobB)).thenReturn(Optional.of(depB));
        // A depends on B, B depends on A — existing cycle in the graph
        when(jobRepository.findDependenciesOf(jobA)).thenReturn(List.of(depB));
        when(jobRepository.findDependenciesOf(jobB)).thenReturn(List.of(depA));
        // give depA and depB recognisable IDs so the DFS can match them
        depA = mockJobWithId(jobA);
        depB = mockJobWithId(jobB);
        when(jobRepository.findById(jobA)).thenReturn(Optional.of(depA));
        when(jobRepository.findById(jobB)).thenReturn(Optional.of(depB));
        when(jobRepository.findDependenciesOf(jobA)).thenReturn(List.of(depB));
        when(jobRepository.findDependenciesOf(jobB)).thenReturn(List.of(depA));

        assertThatThrownBy(() ->
                jobService.submit(clientId, JobType.EMAIL, "{}", 0, 3, List.of(jobA, jobB)))
                .isInstanceOf(CyclicDependencyException.class);
    }

    @Test
    void submit_throwsClientNotFoundException_whenClientMissing() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                jobService.submit(clientId, JobType.EMAIL, "{}", 0, 3, List.of()))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void getStatus_returnsJob_whenExists() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().client(client).type(JobType.EMAIL).payload("{}").build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThat(jobService.getStatus(jobId)).isEqualTo(job);
    }

    @Test
    void getStatus_throwsJobNotFoundException_whenMissing() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getStatus(jobId))
                .isInstanceOf(JobNotFoundException.class);
    }

    // Helper: creates a Job whose getId() returns the given UUID via a real object with id set via reflection
    private Job mockJobWithId(UUID id) {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(id);
        return job;
    }
}
