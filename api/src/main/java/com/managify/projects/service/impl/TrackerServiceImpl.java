package com.managify.projects.service.impl;

import com.managify.config.CacheNames;
import com.managify.exception.AppException;
import com.managify.projects.cache.ProjectCacheEvictor;
import com.managify.projects.dto.request.CreateTrackerItemRequest;
import com.managify.projects.dto.request.UpdateTrackerItemRequest;
import com.managify.projects.dto.response.ProjectTrackerResponse;
import com.managify.projects.enums.ItemStatus;
import com.managify.projects.model.Project;
import com.managify.projects.model.TrackerItem;
import com.managify.projects.model.Worker;
import com.managify.projects.repository.ProjectRepository;
import com.managify.projects.repository.TrackerItemRepository;
import com.managify.projects.repository.WorkerRepository;
import com.managify.projects.service.DateRules;
import com.managify.projects.service.TrackerOrdering;
import com.managify.projects.service.TrackerService;
import com.managify.projects.service.TreeAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Tasks are tracker items without a parent; tasklets have a task as parent. Only two levels exist.
 * Each write evicts the project's cached tree after commit and returns the freshly built tree.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TrackerServiceImpl implements TrackerService {

    private final ProjectRepository projects;

    private final TrackerItemRepository items;

    private final WorkerRepository workers;

    private final ProjectCacheEvictor evictor;

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PROJECT_TRACKER, key = "#projectId")
    public ProjectTrackerResponse getTracker(Long projectId) {
        return build(projectId);
    }

    // ── Create / update / delete ─────────────────────────────────────────────

    @Override
    public ProjectTrackerResponse createTask(Long projectId, CreateTrackerItemRequest request) {
        Project project = project(projectId);
        TrackerItem task = newItem(project, request);
        task.setPosition(tasksOf(projectId).size());
        items.save(task);
        return changed(projectId);
    }

    @Override
    public ProjectTrackerResponse createTasklet(Long projectId, Long taskId, CreateTrackerItemRequest request) {
        Project project = project(projectId);
        TrackerItem task = item(projectId, taskId);
        if (!task.isTask()) {
            throw AppException.invalidHierarchy(
                    "Item " + taskId + " is a tasklet; tasklets cannot have tasklets of their own");
        }
        TrackerItem tasklet = newItem(project, request);
        tasklet.setParent(task);
        tasklet.setPosition(taskletsOf(taskId).size());
        items.save(tasklet);
        return changed(projectId);
    }

    @Override
    public ProjectTrackerResponse update(Long projectId, Long itemId, UpdateTrackerItemRequest request) {
        TrackerItem item = item(projectId, itemId);
        if (request.title() != null) {
            item.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }
        if (request.status() != null) {
            item.setStatus(request.status());
        }
        if (request.startDate() != null) {
            item.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            item.setEndDate(request.endDate());
        }
        DateRules.requireOrdered(item.getStartDate(), item.getEndDate(), label(item));
        return changed(projectId);
    }

    @Override
    public ProjectTrackerResponse delete(Long projectId, Long itemId) {
        TrackerItem item = item(projectId, itemId);
        // Collect evictions while the deleted items' assignees are still in the project.
        evictor.trackerChanged(projectId);
        TrackerOrdering.remove(siblingsOf(item), item);
        // A task's tasklets go with it (ON DELETE CASCADE).
        items.delete(item);
        items.flush();
        return build(projectId);
    }

    // ── Ordering ─────────────────────────────────────────────────────────────

    @Override
    public ProjectTrackerResponse moveUp(Long projectId, Long itemId) {
        TrackerItem item = item(projectId, itemId);
        TrackerOrdering.moveUp(siblingsOf(item), item);
        return changed(projectId);
    }

    @Override
    public ProjectTrackerResponse moveDown(Long projectId, Long itemId) {
        TrackerItem item = item(projectId, itemId);
        TrackerOrdering.moveDown(siblingsOf(item), item);
        return changed(projectId);
    }

    // ── Task ⇄ tasklet ───────────────────────────────────────────────────────

    @Override
    public ProjectTrackerResponse makeTasklet(Long projectId, Long itemId, Long parentTaskId) {
        if (itemId.equals(parentTaskId)) {
            throw AppException.invalidHierarchy("An item cannot become a tasklet of itself");
        }
        TrackerItem item = item(projectId, itemId);
        TrackerItem target = item(projectId, parentTaskId);
        if (!target.isTask()) {
            throw AppException.invalidHierarchy(
                    "Item " + parentTaskId + " is a tasklet; only a task can hold tasklets");
        }
        if (item.isTask() && items.existsByParentId(itemId)) {
            throw AppException.invalidHierarchy("Task " + itemId + " has tasklets of its own. "
                    + "Move them to another task or make them tasks first; only two levels are allowed");
        }
        if (!item.isTask() && item.getParent().getId().equals(parentTaskId)) {
            return build(projectId); // already a tasklet of that task
        }

        TrackerOrdering.remove(siblingsOf(item), item);
        item.setPosition(taskletsOf(parentTaskId).size());
        item.setParent(target);
        return changed(projectId);
    }

    @Override
    public ProjectTrackerResponse makeTask(Long projectId, Long itemId) {
        TrackerItem item = item(projectId, itemId);
        if (item.isTask()) {
            throw AppException.invalidHierarchy("Item " + itemId + " is already a task");
        }
        TrackerOrdering.remove(siblingsOf(item), item);
        item.setPosition(tasksOf(projectId).size());
        item.setParent(null);
        return changed(projectId);
    }

    // ── Assignment ───────────────────────────────────────────────────────────

    @Override
    public ProjectTrackerResponse assign(Long projectId, Long itemId, Long workerId) {
        TrackerItem item = item(projectId, itemId);
        Long previous = item.getAssignee() == null ? null : item.getAssignee().getId();
        item.setAssignee(activeWorker(workerId));
        evictor.trackerChanged(projectId, previous, workerId);
        items.flush();
        return build(projectId);
    }

    @Override
    public ProjectTrackerResponse unassign(Long projectId, Long itemId) {
        TrackerItem item = item(projectId, itemId);
        Long previous = item.getAssignee() == null ? null : item.getAssignee().getId();
        item.setAssignee(null);
        evictor.trackerChanged(projectId, previous);
        items.flush();
        return build(projectId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Evict after commit, then answer with the new tree (built from the flushed state). */
    private ProjectTrackerResponse changed(Long projectId) {
        items.flush();
        evictor.trackerChanged(projectId);
        return build(projectId);
    }

    private ProjectTrackerResponse build(Long projectId) {
        return TreeAssembler.assemble(project(projectId), items.findTreeRows(projectId));
    }

    private TrackerItem newItem(Project project, CreateTrackerItemRequest request) {
        DateRules.requireOrdered(request.startDate(), request.endDate(), "'" + request.title().trim() + "'");
        TrackerItem item = new TrackerItem();
        item.setProject(project);
        item.setTitle(request.title().trim());
        item.setDescription(request.description());
        item.setStatus(request.status() != null ? request.status() : ItemStatus.TODO);
        item.setStartDate(request.startDate());
        item.setEndDate(request.endDate());
        if (request.assigneeId() != null) {
            item.setAssignee(activeWorker(request.assigneeId()));
        }
        return item;
    }

    private List<TrackerItem> siblingsOf(TrackerItem item) {
        return item.isTask()
                ? tasksOf(item.getProject().getId())
                : taskletsOf(item.getParent().getId());
    }

    private List<TrackerItem> tasksOf(Long projectId) {
        return new ArrayList<>(items.findByProjectIdAndParentIsNullOrderByPositionAscIdAsc(projectId));
    }

    private List<TrackerItem> taskletsOf(Long taskId) {
        return new ArrayList<>(items.findByParentIdOrderByPositionAscIdAsc(taskId));
    }

    private Project project(Long projectId) {
        return projects.findById(projectId).orElseThrow(() -> AppException.projectNotFound(projectId));
    }

    private TrackerItem item(Long projectId, Long itemId) {
        return items.findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> AppException.trackerItemNotFound(projectId, itemId));
    }

    private Worker activeWorker(Long workerId) {
        Worker worker = workers.findById(workerId).orElseThrow(() -> AppException.workerNotFound(workerId));
        if (!worker.isActive()) {
            throw AppException.workerInactive(workerId);
        }
        return worker;
    }

    private static String label(TrackerItem item) {
        return (item.isTask() ? "Task" : "Tasklet") + " '" + item.getTitle() + "'";
    }
}
