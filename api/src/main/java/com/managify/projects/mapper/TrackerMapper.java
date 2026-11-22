package com.managify.projects.mapper;

import com.managify.projects.dto.response.AssignedItemResponse;
import com.managify.projects.dto.response.ProjectResponse;
import com.managify.projects.dto.response.WorkerResponse;
import com.managify.projects.dto.response.WorkerSummary;
import com.managify.projects.model.Project;
import com.managify.projects.model.TrackerItem;
import com.managify.projects.model.Worker;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Entity → response DTO mapping for the project module. */
public final class TrackerMapper {

    private TrackerMapper() {}

    /** Inclusive day count between two dates; null when either is missing. */
    public static Long durationDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    public static ProjectResponse toResponse(Project p) {
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getStartDate(), p.getEndDate(), durationDays(p.getStartDate(), p.getEndDate()),
                p.getStatus(), p.getCreatedBy(), p.getDateCreated(), p.getDateLastUpdated());
    }

    public static WorkerResponse toResponse(Worker w) {
        return new WorkerResponse(w.getId(), w.getFullName(), w.getEmail(), w.getKeycloakUserId(), w.isActive());
    }

    public static WorkerSummary toSummary(Worker w) {
        return w == null ? null : new WorkerSummary(w.getId(), w.getFullName(), w.isActive());
    }

    public static AssignedItemResponse toAssigned(TrackerItem i) {
        TrackerItem parent = i.getParent();
        return new AssignedItemResponse(
                i.getId(), i.getType(), i.getTitle(), i.getStatus(), i.getStartDate(), i.getEndDate(),
                i.getProject().getId(), i.getProject().getName(),
                parent == null ? null : parent.getId(),
                parent == null ? null : parent.getTitle());
    }
}
