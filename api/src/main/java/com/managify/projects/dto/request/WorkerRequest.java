package com.managify.projects.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create or fully replace a worker. */
public record WorkerRequest(

        @NotBlank @Size(max = 150)
        String fullName,

        @NotBlank @Email @Size(max = 200)
        String email,

        @Size(max = 64)
        @Schema(description = "Optional Keycloak user id (token sub)")
        String keycloakUserId,

        @Schema(description = "Defaults to true; inactive workers cannot be assigned")
        Boolean active
) {}
