package com.nevis.search.service;

import com.nevis.search.api.CreateClientRequest;
import com.nevis.search.domain.ClientEntity;
import com.nevis.search.domain.ClientRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientService {

    private final ClientRepository clients;

    public ClientService(ClientRepository clients) {
        this.clients = clients;
    }

    public ClientEntity create(CreateClientRequest req) {
        String email = req.email().strip().toLowerCase(Locale.ROOT);
        if (clients.existsByEmail(email)) throw duplicate();
        List<String> links = req.socialLinks() == null ? List.of() : req.socialLinks().stream().map(String::strip).toList();
        ClientEntity entity = new ClientEntity(
                req.firstName().strip(), req.lastName().strip(), email,
                req.description() == null ? null : req.description().strip(), links);
        try {
            return clients.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) { // lost a race with a concurrent insert
            throw duplicate();
        }
    }

    public ClientEntity get(String id) {
        return clients.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    private static ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Client with this email already exists");
    }
}
