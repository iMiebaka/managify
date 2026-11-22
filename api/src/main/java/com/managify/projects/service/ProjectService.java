package com.managify.projects.service;

import com.managify.dto.response.PagedResponse;
import com.managify.projects.dto.request.ProjectRequest;
import com.managify.projects.dto.response.ProjectResponse;

public interface ProjectService {

    ProjectResponse create(ProjectRequest request);

    /** {@code page} is 1-based. */
    PagedResponse<ProjectResponse> list(int page, int size);

    ProjectResponse get(Long id);

    ProjectResponse update(Long id, ProjectRequest request);

    void delete(Long id);
}
