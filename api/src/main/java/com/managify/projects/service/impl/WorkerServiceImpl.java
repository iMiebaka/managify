package com.managify.projects.service.impl;

import com.managify.config.CacheNames;
import com.managify.dto.response.PagedResponse;
import com.managify.exception.AppException;
import com.managify.projects.cache.ProjectCacheEvictor;
import com.managify.projects.dto.request.WorkerRequest;
import com.managify.projects.dto.response.WorkerItemsResponse;
import com.managify.projects.dto.response.WorkerResponse;
import com.managify.projects.mapper.TrackerMapper;
import com.managify.projects.model.Worker;
import com.managify.projects.repository.TrackerItemRepository;
import com.managify.projects.repository.WorkerRepository;
import com.managify.projects.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerServiceImpl implements WorkerService {

    private final WorkerRepository workers;

    private final TrackerItemRepository items;

    private final ProjectCacheEvictor evictor;

    @Override
    public WorkerResponse create(WorkerRequest request) {
        String email = request.email().trim();
        if (workers.existsByEmailIgnoreCase(email)) {
            throw AppException.duplicateWorkerEmail(email);
        }
        Worker worker = new Worker();
        apply(worker, request, email);
        worker.setActive(request.active() == null || request.active());
        return TrackerMapper.toResponse(workers.save(worker));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkerResponse> list(int page, int size) {
        return PagedResponse.of(workers
                .findAll(PageRequest.of(page - 1, size, Sort.by("fullName").ascending()))
                .map(TrackerMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.WORKER, key = "#id")
    public WorkerResponse get(Long id) {
        return TrackerMapper.toResponse(find(id));
    }

    @Override
    public WorkerResponse update(Long id, WorkerRequest request) {
        Worker worker = find(id);
        String email = request.email().trim();
        if (workers.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw AppException.duplicateWorkerEmail(email);
        }
        apply(worker, request, email);
        if (request.active() != null) {
            worker.setActive(request.active());
        }
        // The worker's name shows on every tracker they are assigned in.
        evictor.workerChanged(id);
        return TrackerMapper.toResponse(worker);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.WORKER_ITEMS, key = "#workerId")
    public WorkerItemsResponse assignedItems(Long workerId) {
        find(workerId);
        return new WorkerItemsResponse(workerId,
                items.findAssignedTo(workerId).stream().map(TrackerMapper::toAssigned).toList());
    }

    private Worker find(Long id) {
        return workers.findById(id).orElseThrow(() -> AppException.workerNotFound(id));
    }

    private static void apply(Worker worker, WorkerRequest request, String email) {
        worker.setFullName(request.fullName().trim());
        worker.setEmail(email);
        worker.setKeycloakUserId(request.keycloakUserId() == null || request.keycloakUserId().isBlank()
                ? null : request.keycloakUserId().trim());
    }
}
