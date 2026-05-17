package com.forge.api.controller;

import com.forge.api.dto.ClientResponse;
import com.forge.api.dto.CreateClientRequest;
import com.forge.application.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse register(@RequestBody CreateClientRequest request) {
        return ClientResponse.from(
                clientService.register(request.getName(), request.getApiKey(), request.getMaxConcurrentJobs())
        );
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable UUID id) {
        return ClientResponse.from(clientService.findById(id));
    }
}
