package com.managify.projects.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Turn a task into a tasklet of another task in the same project. */
public record MakeTaskletRequest(

        @NotNull
        @Schema(description = "The task that becomes the parent")
        Long parentTaskId
) {}
