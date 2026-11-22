package com.managify.projects.dto.request;

import com.managify.projects.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Create or fully replace a project. The duration is startDate to endDate, both inclusive. */
public record ProjectRequest(

        @NotBlank @Size(max = 150)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        @Schema(example = "2026-12-01")
        LocalDate startDate,

        @NotNull
        @Schema(example = "2027-03-31")
        LocalDate endDate,

        @Schema(description = "Defaults to PLANNED on create; unchanged on update when omitted")
        ProjectStatus status
) {}
