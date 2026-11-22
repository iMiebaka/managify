package com.managify.projects.controller;

import com.managify.config.auth.Roles;
import com.managify.dto.response.ApiResult;
import com.managify.projects.dto.request.AssignWorkerRequest;
import com.managify.projects.dto.request.CreateTrackerItemRequest;
import com.managify.projects.dto.request.MakeTaskletRequest;
import com.managify.projects.dto.request.UpdateTrackerItemRequest;
import com.managify.projects.dto.response.ProjectTrackerResponse;
import com.managify.projects.service.TrackerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tasks and tasklets of a project's tracker. Every call returns the updated tracker tree. */
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
@PreAuthorize(Roles.HAS_USER_OR_ADMIN)
@Tag(name = "Project tracker", description = "Tasks, tasklets, ordering and assignment")
public class TrackerController {

    private final TrackerService trackerService;

    @PostMapping("/tasks")
    @Operation(summary = "Add a task", description = "Added after the project's last task")
    public ResponseEntity<ApiResult<ProjectTrackerResponse>> createTask(
            @PathVariable Long projectId, @Valid @RequestBody CreateTrackerItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Task added", trackerService.createTask(projectId, request)));
    }

    @PostMapping("/tasks/{taskId}/tasklets")
    @Operation(summary = "Add a tasklet to a task", description = "Added after the task's last tasklet")
    public ResponseEntity<ApiResult<ProjectTrackerResponse>> createTasklet(
            @PathVariable Long projectId, @PathVariable Long taskId,
            @Valid @RequestBody CreateTrackerItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Tasklet added", trackerService.createTasklet(projectId, taskId, request)));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update a task or tasklet", description = "Only the fields sent are changed")
    public ApiResult<ProjectTrackerResponse> update(@PathVariable Long projectId, @PathVariable Long itemId,
                                                    @Valid @RequestBody UpdateTrackerItemRequest request) {
        return ApiResult.success("Item updated", trackerService.update(projectId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Delete a task or tasklet", description = "Deleting a task deletes its tasklets")
    public ApiResult<ProjectTrackerResponse> delete(@PathVariable Long projectId, @PathVariable Long itemId) {
        return ApiResult.success("Item deleted", trackerService.delete(projectId, itemId));
    }

    @PostMapping("/items/{itemId}/move-up")
    @Operation(summary = "Move up", description = "Swaps with the sibling above; no change when already first")
    public ApiResult<ProjectTrackerResponse> moveUp(@PathVariable Long projectId, @PathVariable Long itemId) {
        return ApiResult.success(trackerService.moveUp(projectId, itemId));
    }

    @PostMapping("/items/{itemId}/move-down")
    @Operation(summary = "Move down", description = "Swaps with the sibling below; no change when already last")
    public ApiResult<ProjectTrackerResponse> moveDown(@PathVariable Long projectId, @PathVariable Long itemId) {
        return ApiResult.success(trackerService.moveDown(projectId, itemId));
    }

    @PostMapping("/items/{itemId}/make-tasklet")
    @Operation(summary = "Make it a tasklet of another task",
            description = "A task without tasklets becomes a tasklet; a tasklet moves to another task. "
                    + "Added last under the new parent. 409 for a task that still has tasklets.")
    public ApiResult<ProjectTrackerResponse> makeTasklet(@PathVariable Long projectId, @PathVariable Long itemId,
                                                         @Valid @RequestBody MakeTaskletRequest request) {
        return ApiResult.success(trackerService.makeTasklet(projectId, itemId, request.parentTaskId()));
    }

    @PostMapping("/items/{itemId}/make-task")
    @Operation(summary = "Make a tasklet a task", description = "Placed after the project's last task")
    public ApiResult<ProjectTrackerResponse> makeTask(@PathVariable Long projectId, @PathVariable Long itemId) {
        return ApiResult.success(trackerService.makeTask(projectId, itemId));
    }

    @PutMapping("/items/{itemId}/assignee")
    @Operation(summary = "Assign a worker", description = "Replaces any current assignee; the worker must be active")
    public ApiResult<ProjectTrackerResponse> assign(@PathVariable Long projectId, @PathVariable Long itemId,
                                                    @Valid @RequestBody AssignWorkerRequest request) {
        return ApiResult.success("Worker assigned", trackerService.assign(projectId, itemId, request.workerId()));
    }

    @DeleteMapping("/items/{itemId}/assignee")
    @Operation(summary = "Remove the assignee")
    public ApiResult<ProjectTrackerResponse> unassign(@PathVariable Long projectId, @PathVariable Long itemId) {
        return ApiResult.success("Worker unassigned", trackerService.unassign(projectId, itemId));
    }
}
