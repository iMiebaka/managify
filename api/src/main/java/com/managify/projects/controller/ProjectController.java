package com.managify.projects.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;

import com.managify.config.auth.Roles;
import com.managify.dto.response.ApiResult;
import com.managify.dto.response.PagedResponse;
import com.managify.projects.service.ProjectService;
import com.managify.projects.service.TrackerService;
import com.managify.projects.dto.request.ProjectRequest;
import com.managify.projects.dto.response.ProjectResponse;

import com.managify.projects.dto.response.ProjectTrackerResponse;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@PreAuthorize(Roles.HAS_USER_OR_ADMIN)
@Tag(name = "Projects", description = "Projects: name, duration and status")
public class ProjectController {

    private final ProjectService projectService;

    private final TrackerService trackerService;

    @Value("${app.page-size:10}")
    private int defaultPageSize;

    @PostMapping
    @Operation(summary = "Create a project")
    public ResponseEntity<ApiResult<ProjectResponse>> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Project created", projectService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List projects", description = "Paged, sorted by name. page is 1-based.")
    public ApiResult<PagedResponse<ProjectResponse>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size) {
        return ApiResult.success(projectService.list(page, Paging.size(page, size, defaultPageSize)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project", description = "Cached")
    public ApiResult<ProjectResponse> get(@PathVariable Long id) {
        return ApiResult.success(projectService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project", description = "Replaces name, description and dates; status only when sent")
    public ApiResult<ProjectResponse> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return ApiResult.success("Project updated", projectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project", description = "Deletes its tasks and tasklets too")
    public ApiResult<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResult.message("Project deleted");
    }

    @GetMapping("/{id}/tracker")
    @Operation(summary = "Get the project tracker", description = "The whole tree: tasks in order, each with its tasklets, assignees and dates. Cached.")
    public ApiResult<ProjectTrackerResponse> tracker(@PathVariable Long id) {
        return ApiResult.success(trackerService.getTracker(id));
    }
}
