package com.nevis.search.api;

import com.nevis.search.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @Operation(summary = "Create a client", description = "201 on success, 409 if the email already exists, 422 if the body is invalid.")
    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody CreateClientRequest request) {
        return ClientResponse.from(service.create(request));
    }

    @Operation(summary = "Get a client", description = "404 if the client does not exist.")
    @GetMapping("/clients/{id}")
    public ClientResponse get(@PathVariable String id) {
        return ClientResponse.from(service.get(id));
    }
}
