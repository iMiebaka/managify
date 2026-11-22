package com.managify.projects.service.impl;

import com.managify.config.CacheNames;
import com.managify.dto.response.PagedResponse;
import com.managify.exception.AppException;
import com.managify.projects.cache.ProjectCacheEvictor;
import com.managify.projects.dto.request.ProjectRequest;
import com.managify.projects.dto.response.ProjectResponse;
import com.managify.projects.enums.ProjectStatus;
import com.managify.projects.mapper.TrackerMapper;
import com.managify.projects.model.Project;
import com.managify.projects.repository.ProjectRepository;
import com.managify.projects.service.DateRules;
import com.managify.projects.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projects;

    private final ProjectCacheEvictor evictor;

    @Override
    public ProjectResponse create(ProjectRequest request) {
        String name = request.name().trim();
        if (projects.existsByNameIgnoreCase(name)) {
            throw AppException.duplicateProjectName(name);
        }
        requireDates(request);

        Project project = new Project();
        apply(project, request, name);
        project.setStatus(request.status() != null ? request.status() : ProjectStatus.PLANNED);
        return TrackerMapper.toResponse(projects.save(project));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProjectResponse> list(int page, int size) {
        return PagedResponse.of(projects
                .findAll(PageRequest.of(page - 1, size, Sort.by("name").ascending()))
                .map(TrackerMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PROJECT, key = "#id")
    public ProjectResponse get(Long id) {
        return TrackerMapper.toResponse(find(id));
    }

    @Override
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = find(id);
        String name = request.name().trim();
        if (projects.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw AppException.duplicateProjectName(name);
        }
        requireDates(request);

        apply(project, request, name);
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        evictor.projectChanged(id);
        return TrackerMapper.toResponse(project);
    }

    @Override
    public void delete(Long id) {
        Project project = find(id);
        // Collect evictions before the items (and their assignees) are gone.
        evictor.projectChanged(id);
        // tracker_items rows go with it (ON DELETE CASCADE).
        projects.delete(project);
    }

    private Project find(Long id) {
        return projects.findById(id).orElseThrow(() -> AppException.projectNotFound(id));
    }

    private static void requireDates(ProjectRequest request) {
        DateRules.requireOrdered(request.startDate(), request.endDate(), "The project");
    }

    private static void apply(Project project, ProjectRequest request, String name) {
        project.setName(name);
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
    }
}
