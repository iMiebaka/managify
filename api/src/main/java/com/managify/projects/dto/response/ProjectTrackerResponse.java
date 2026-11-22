package com.managify.projects.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** A project with its whole tracker: tasks in order, each with its tasklets in order. */
public record ProjectTrackerResponse(
        ProjectResponse project,
        List<TrackerItemNode> tasks,
        int taskCount,
        int taskletCount,
        @Schema(description = "Items whose dates fall outside the project's dates. Informational only.")
        List<String> warnings
) {}
