package com.nevis.search.core;

import java.util.List;

/** What the client matcher needs. Keeps the core free of JPA and Spring. */
public record ClientData(
        String id, String firstName, String lastName, String email,
        String description, List<String> socialLinks) {}
