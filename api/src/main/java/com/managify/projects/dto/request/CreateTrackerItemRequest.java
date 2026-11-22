package com.managify.projects.dto.request;

import com.managify.projects.enums.ItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** A new task or tasklet. It is added at the end of its siblings. */
public record CreateTrackerItemRequest(

        @NotBlank @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        @Schema(description = "Defaults to TODO")
        ItemStatus status,

        LocalDate startDate,

        LocalDate endDate,

        @Schema(description = "Optional worker to assign straight away")
        Long assigneeId
) {}
