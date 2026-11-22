package com.managify.projects.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignWorkerRequest(@NotNull Long workerId) {}
