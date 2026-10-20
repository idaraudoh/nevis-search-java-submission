package com.nevis.search.api;

import com.nevis.search.domain.DocumentEntity;
import java.time.Instant;

public record DocumentResponse(String id, String clientId, String title, String content, Instant createdAt) {

    public static DocumentResponse from(DocumentEntity d) {
        return new DocumentResponse(d.getId(), d.getClientId(), d.getTitle(), d.getContent(), d.getCreatedAt());
    }
}
