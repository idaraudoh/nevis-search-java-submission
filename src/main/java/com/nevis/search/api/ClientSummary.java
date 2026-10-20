package com.nevis.search.api;

import com.nevis.search.domain.ClientEntity;

/** Just enough to tell an advisor whose document a hit is. The full record is at GET /clients/{id}. */
public record ClientSummary(String id, String firstName, String lastName, String email) {

    public static ClientSummary from(ClientEntity c) {
        return c == null ? null : new ClientSummary(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail());
    }
}
