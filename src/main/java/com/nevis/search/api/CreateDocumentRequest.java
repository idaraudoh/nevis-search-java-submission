package com.nevis.search.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 200_000) String content) {}
