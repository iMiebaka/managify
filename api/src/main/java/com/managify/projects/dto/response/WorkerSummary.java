package com.managify.projects.dto.response;

/** The assignee shown on a tracker item. */
public record WorkerSummary(Long id, String fullName, boolean active) {}
