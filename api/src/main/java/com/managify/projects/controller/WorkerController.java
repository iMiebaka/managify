package com.managify.projects.controller;

import com.managify.config.auth.Roles;
import com.managify.dto.response.ApiResult;
import com.managify.dto.response.PagedResponse;
import com.managify.projects.dto.request.WorkerRequest;
import com.managify.projects.dto.response.WorkerItemsResponse;
import com.managify.projects.dto.response.WorkerResponse;
import com.managify.projects.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
@PreAuthorize(Roles.HAS_USER_OR_ADMIN)
@Tag(name = "Workers", description = "People who are assigned tasks and tasklets")
public class WorkerController {

    private final WorkerService workerService;

    @Value("${app.page-size:10}")
    private int defaultPageSize;

    @PostMapping
    @Operation(summary = "Add a worker")
    public ResponseEntity<ApiResult<WorkerResponse>> create(@Valid @RequestBody WorkerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Worker added", workerService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List workers", description = "Paged, sorted by name. page is 1-based.")
    public ApiResult<PagedResponse<WorkerResponse>> list(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(required = false) Integer size) {
        return ApiResult.success(workerService.list(page, Paging.size(page, size, defaultPageSize)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a worker", description = "Cached")
    public ApiResult<WorkerResponse> get(@PathVariable Long id) {
        return ApiResult.success(workerService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a worker", description = "Set active=false to stop new assignments")
    public ApiResult<WorkerResponse> update(@PathVariable Long id, @Valid @RequestBody WorkerRequest request) {
        return ApiResult.success("Worker updated", workerService.update(id, request));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "What a worker is assigned to", description = "Tasks and tasklets across projects. Cached.")
    public ApiResult<WorkerItemsResponse> items(@PathVariable Long id) {
        return ApiResult.success(workerService.assignedItems(id));
    }
}
