package com.forge.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.forge.domain.model.Client;

public interface IClientRepository {
    // Basic CRUD
    Optional<Client> findById(UUID id);

    Client save(Client client);

    // Auth — API key lookup on every request
    Optional<Client> findByApiKey(String apiKey);
}
