package com.forge.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forge.domain.model.Client;
import com.forge.domain.repository.IClientRepository;

public interface JpaClientRepository extends JpaRepository<Client, UUID>, IClientRepository {

    Optional<Client> findByApiKey(String apiKey);
}
