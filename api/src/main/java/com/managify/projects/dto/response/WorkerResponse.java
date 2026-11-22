package com.managify.projects.dto.response;

public record WorkerResponse(
        Long id,
        String fullName,
        String email,
        String keycloakUserId,
        String imageUrl,
        boolean active
) {}
