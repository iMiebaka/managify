package com.managify.projects.dto.request;

import com.managify.projects.enums.ItemStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Partial update: only the fields sent are changed. */
public record UpdateTrackerItemRequest(

        @Size(max = 200)
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String title,

        @Size(max = 2000)
        String description,

        ItemStatus status,

        LocalDate startDate,

        LocalDate endDate
) {}
