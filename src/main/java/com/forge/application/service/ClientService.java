package com.forge.application.service;

import com.forge.domain.model.Client;
import com.forge.domain.repository.IClientRepository;
import com.forge.shared.exception.ClientNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ClientService {

    private final IClientRepository clientRepository;

    public ClientService(IClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client register(String name, String apiKey, int maxConcurrentJobs) {
        Client client = Client.builder()
                .name(name)
                .apiKey(apiKey)
                .maxConcurrentJobs(maxConcurrentJobs)
                .build();
        return clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public Client findById(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }
}
