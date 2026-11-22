package com.managify.projects.dto.response;

import com.managify.projects.enums.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        /** endDate − startDate + 1. */
        Long durationDays,
        ProjectStatus status,
        String createdBy,
        LocalDateTime dateCreated,
        LocalDateTime dateLastUpdated
) {}
