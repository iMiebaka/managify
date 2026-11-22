package com.managify.projects.dto.response;

import com.managify.projects.enums.ItemStatus;
import com.managify.projects.enums.ItemType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** A task (with its tasklets) or a tasklet (with an empty tasklets list) in the tracker tree. */
public record TrackerItemNode(
        Long id,
        ItemType type,
        String title,
        String description,
        ItemStatus status,
        @Schema(description = "For a task with tasklets: the earliest tasklet start")
        LocalDate startDate,
        @Schema(description = "For a task with tasklets: the latest tasklet end")
        LocalDate endDate,
        @Schema(description = "endDate − startDate + 1; null when either date is missing")
        Long durationDays,
        @Schema(description = "True when the dates come from the tasklets rather than the task itself")
        boolean datesDerived,
        int position,
        WorkerSummary assignee,
        List<TrackerItemNode> tasklets
) {}
