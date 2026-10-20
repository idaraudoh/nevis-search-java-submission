package com.nevis.search.api;

import com.nevis.search.domain.ClientEntity;
import java.time.Instant;
import java.util.List;

public record ClientResponse(
        String id, String firstName, String lastName, String email,
        String description, List<String> socialLinks, Instant createdAt) {

    public static ClientResponse from(ClientEntity c) {
        return new ClientResponse(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(),
                c.getDescription(), List.copyOf(c.getSocialLinks()), c.getCreatedAt());
    }
}
