package com.forge.application.service;

import com.forge.domain.model.Client;
import com.forge.domain.repository.IClientRepository;
import com.forge.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    IClientRepository clientRepository;

    @InjectMocks
    ClientService clientService;

    @Test
    void register_savesAndReturnsClient() {
        Client saved = Client.builder().name("Acme").apiKey("key-1").maxConcurrentJobs(5).build();
        when(clientRepository.save(any(Client.class))).thenReturn(saved);

        Client result = clientService.register("Acme", "key-1", 5);

        assertThat(result.getName()).isEqualTo("Acme");
        assertThat(result.getApiKey()).isEqualTo("key-1");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void findById_returnsClient_whenExists() {
        UUID id = UUID.randomUUID();
        Client client = Client.builder().name("Acme").apiKey("key-1").build();
        when(clientRepository.findById(id)).thenReturn(Optional.of(client));

        Client result = clientService.findById(id);

        assertThat(result).isEqualTo(client);
    }

    @Test
    void findById_throwsClientNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.findById(id))
                .isInstanceOf(ClientNotFoundException.class);
    }
}
