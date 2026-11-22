package com.managify.projects.dto.response;

import com.managify.projects.enums.ItemStatus;
import com.managify.projects.enums.ItemType;

import java.time.LocalDate;

/** A task or tasklet assigned to a worker, with where it sits. */
public record AssignedItemResponse(
        Long id,
        ItemType type,
        String title,
        ItemStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Long projectId,
        String projectName,
        Long parentTaskId,
        String parentTaskTitle
) {}
