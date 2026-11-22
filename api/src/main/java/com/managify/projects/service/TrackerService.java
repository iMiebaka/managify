package com.managify.projects.service;

import com.managify.projects.dto.request.CreateTrackerItemRequest;
import com.managify.projects.dto.request.UpdateTrackerItemRequest;
import com.managify.projects.dto.response.ProjectTrackerResponse;

/**
 * The project tracker: tasks and their tasklets. Every write returns the updated tree.
 */
public interface TrackerService {

    /** The whole tree, served from Redis when cached. */
    ProjectTrackerResponse getTracker(Long projectId);

    ProjectTrackerResponse createTask(Long projectId, CreateTrackerItemRequest request);

    ProjectTrackerResponse createTasklet(Long projectId, Long taskId, CreateTrackerItemRequest request);

    ProjectTrackerResponse update(Long projectId, Long itemId, UpdateTrackerItemRequest request);

    /** Deleting a task deletes its tasklets too. */
    ProjectTrackerResponse delete(Long projectId, Long itemId);

    ProjectTrackerResponse moveUp(Long projectId, Long itemId);

    ProjectTrackerResponse moveDown(Long projectId, Long itemId);

    /**
     * Puts the item under {@code parentTaskId} as its last tasklet. Works for a task with no tasklets
     * (it becomes a tasklet) and for a tasklet (it moves to another task).
     */
    ProjectTrackerResponse makeTasklet(Long projectId, Long itemId, Long parentTaskId);

    /** Turns a tasklet into a task, placed last among the project's tasks. */
    ProjectTrackerResponse makeTask(Long projectId, Long itemId);

    ProjectTrackerResponse assign(Long projectId, Long itemId, Long workerId);

    ProjectTrackerResponse unassign(Long projectId, Long itemId);
}
