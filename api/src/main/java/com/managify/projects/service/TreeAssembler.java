package com.managify.projects.service;

import com.managify.projects.dto.response.ProjectResponse;
import com.managify.projects.dto.response.ProjectTrackerResponse;
import com.managify.projects.dto.response.TrackerItemNode;
import com.managify.projects.enums.ItemType;
import com.managify.projects.mapper.TrackerMapper;
import com.managify.projects.model.Project;
import com.managify.projects.model.TrackerItem;

import java.time.LocalDate;
import java.util.*;

/**
 * Builds the tracker tree from a project's flat item rows (pure, no database access).
 *
 * <p>Dates: a tasklet shows its own. A task with dated tasklets shows the earliest tasklet start and
 * the latest tasklet end ({@code datesDerived = true}); a side no tasklet sets falls back to the task's own.
 */
public final class TreeAssembler {

    private static final Comparator<TrackerItem> ORDER =
            Comparator.comparingInt(TrackerItem::getPosition).thenComparing(TrackerItem::getId,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private TreeAssembler() {}

    public static ProjectTrackerResponse assemble(Project project, List<TrackerItem> rows) {
        List<TrackerItem> tasks = new ArrayList<>();
        Map<Long, List<TrackerItem>> taskletsByTask = new HashMap<>();
        for (TrackerItem row : rows) {
            if (row.isTask()) {
                tasks.add(row);
            } else {
                taskletsByTask.computeIfAbsent(row.getParent().getId(), k -> new ArrayList<>()).add(row);
            }
        }
        tasks.sort(ORDER);

        ProjectResponse projectResponse = TrackerMapper.toResponse(project);
        List<String> warnings = new ArrayList<>();
        List<TrackerItemNode> taskNodes = new ArrayList<>(tasks.size());
        int taskletCount = 0;

        for (TrackerItem task : tasks) {
            List<TrackerItem> children = taskletsByTask.getOrDefault(task.getId(), List.of());
            List<TrackerItemNode> taskletNodes = children.stream().sorted(ORDER).map(TreeAssembler::tasklet).toList();
            taskletCount += taskletNodes.size();
            TrackerItemNode taskNode = task(task, taskletNodes);
            taskNodes.add(taskNode);

            checkWithinProject(taskNode, project, warnings);
            taskletNodes.forEach(t -> checkWithinProject(t, project, warnings));
        }

        return new ProjectTrackerResponse(projectResponse, taskNodes, taskNodes.size(), taskletCount, warnings);
    }

    private static TrackerItemNode tasklet(TrackerItem i) {
        return new TrackerItemNode(i.getId(), ItemType.TASKLET, i.getTitle(), i.getDescription(), i.getStatus(),
                i.getStartDate(), i.getEndDate(), TrackerMapper.durationDays(i.getStartDate(), i.getEndDate()),
                false, i.getPosition(), TrackerMapper.toSummary(i.getAssignee()), List.of());
    }

    private static TrackerItemNode task(TrackerItem i, List<TrackerItemNode> tasklets) {
        LocalDate earliestStart = tasklets.stream().map(TrackerItemNode::startDate)
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        LocalDate latestEnd = tasklets.stream().map(TrackerItemNode::endDate)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);

        boolean derived = earliestStart != null || latestEnd != null;
        LocalDate start = earliestStart != null ? earliestStart : i.getStartDate();
        LocalDate end = latestEnd != null ? latestEnd : i.getEndDate();

        return new TrackerItemNode(i.getId(), ItemType.TASK, i.getTitle(), i.getDescription(), i.getStatus(),
                start, end, TrackerMapper.durationDays(start, end), derived, i.getPosition(),
                TrackerMapper.toSummary(i.getAssignee()), tasklets);
    }

    private static void checkWithinProject(TrackerItemNode node, Project project, List<String> warnings) {
        String label = (node.type() == ItemType.TASK ? "Task" : "Tasklet") + " '" + node.title() + "'";
        if (node.startDate() != null && node.startDate().isBefore(project.getStartDate())) {
            warnings.add(label + " starts " + node.startDate() + ", before the project starts (" + project.getStartDate() + ")");
        }
        if (node.endDate() != null && node.endDate().isAfter(project.getEndDate())) {
            warnings.add(label + " ends " + node.endDate() + ", after the project ends (" + project.getEndDate() + ")");
        }
    }
}
