package com.managify.projects.service;

import com.managify.dto.response.PagedResponse;
import com.managify.projects.dto.request.WorkerRequest;
import com.managify.projects.dto.response.WorkerItemsResponse;
import com.managify.projects.dto.response.WorkerResponse;

public interface WorkerService {

    WorkerResponse create(WorkerRequest request);

    /** {@code page} is 1-based. */
    PagedResponse<WorkerResponse> list(int page, int size);

    WorkerResponse get(Long id);

    WorkerResponse update(Long id, WorkerRequest request);

    /** Every task and tasklet assigned to the worker, across projects. */
    WorkerItemsResponse assignedItems(Long workerId);
}
