package com.managify.projects.dto.response;

import java.util.List;

public record WorkerItemsResponse(Long workerId, List<AssignedItemResponse> items) {}
