package com.nevis.search.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateClientRequest(
        @NotBlank @Size(max = 200) String firstName,
        @NotBlank @Size(max = 200) String lastName,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 5000) String description,
        @Size(max = 20) List<@NotBlank @Size(max = 1000) String> socialLinks) {}
