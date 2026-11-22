package com.managify.projects.service;

import com.managify.projects.dto.response.ProjectTrackerResponse;
import com.managify.projects.dto.response.TrackerItemNode;
import com.managify.projects.enums.ItemStatus;
import com.managify.projects.enums.ItemType;
import com.managify.projects.enums.ProjectStatus;
import com.managify.projects.model.Project;
import com.managify.projects.model.TrackerItem;
import com.managify.projects.model.Worker;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TreeAssemblerTest {

    private static final LocalDate JAN_1 = LocalDate.of(2027, 1, 1);

    private final Project project = project();

    @Test
    void taskWithTaskletsSpansEarliestStartToLatestEnd() {
        TrackerItem task = item(1L, null, 0, JAN_1, JAN_1.plusDays(1));
        TrackerItem early = item(2L, task, 0, JAN_1.plusDays(3), JAN_1.plusDays(5));
        TrackerItem late = item(3L, task, 1, JAN_1.plusDays(4), JAN_1.plusDays(12));

        ProjectTrackerResponse tree = TreeAssembler.assemble(project, List.of(task, late, early));

        TrackerItemNode node = tree.tasks().get(0);
        assertThat(node.type()).isEqualTo(ItemType.TASK);
        assertThat(node.datesDerived()).isTrue();
        assertThat(node.startDate()).isEqualTo(JAN_1.plusDays(3));
        assertThat(node.endDate()).isEqualTo(JAN_1.plusDays(12));
        assertThat(node.durationDays()).isEqualTo(10L);
        assertThat(node.tasklets()).extracting(TrackerItemNode::id).containsExactly(2L, 3L);
        assertThat(tree.taskCount()).isEqualTo(1);
        assertThat(tree.taskletCount()).isEqualTo(2);
    }

    @Test
    void taskWithoutTaskletsKeepsItsOwnDates() {
        TrackerItem task = item(1L, null, 0, JAN_1, JAN_1.plusDays(6));

        TrackerItemNode node = TreeAssembler.assemble(project, List.of(task)).tasks().get(0);

        assertThat(node.datesDerived()).isFalse();
        assertThat(node.startDate()).isEqualTo(JAN_1);
        assertThat(node.durationDays()).isEqualTo(7L);
        assertThat(node.tasklets()).isEmpty();
    }

    @Test
    void undatedTaskletsLeaveTheTaskDatesAlone() {
        TrackerItem task = item(1L, null, 0, JAN_1, JAN_1.plusDays(2));
        TrackerItem undated = item(2L, task, 0, null, null);

        TrackerItemNode node = TreeAssembler.assemble(project, List.of(task, undated)).tasks().get(0);

        assertThat(node.datesDerived()).isFalse();
        assertThat(node.endDate()).isEqualTo(JAN_1.plusDays(2));
        assertThat(node.tasklets().get(0).durationDays()).isNull();
    }

    @Test
    void tasksAreOrderedByPositionAndCarryTheirAssignee() {
        Worker ada = new Worker();
        ada.setId(7L);
        ada.setFullName("Ada");
        TrackerItem second = item(1L, null, 1, null, null);
        TrackerItem first = item(2L, null, 0, null, null);
        first.setAssignee(ada);

        ProjectTrackerResponse tree = TreeAssembler.assemble(project, List.of(second, first));

        assertThat(tree.tasks()).extracting(TrackerItemNode::id).containsExactly(2L, 1L);
        assertThat(tree.tasks().get(0).assignee().fullName()).isEqualTo("Ada");
        assertThat(tree.tasks().get(1).assignee()).isNull();
    }

    @Test
    void itemsOutsideTheProjectDatesAreWarnedAbout() {
        TrackerItem task = item(1L, null, 0, JAN_1.minusDays(1), JAN_1.plusDays(200));

        ProjectTrackerResponse tree = TreeAssembler.assemble(project, List.of(task));

        assertThat(tree.warnings()).hasSize(2);
        assertThat(tree.project().durationDays()).isEqualTo(90L);
    }

    private static Project project() {
        Project p = new Project();
        p.setId(100L);
        p.setName("Office fit-out");
        p.setStartDate(JAN_1);
        p.setEndDate(JAN_1.plusDays(89));
        p.setStatus(ProjectStatus.ACTIVE);
        return p;
    }

    private TrackerItem item(Long id, TrackerItem parent, int position, LocalDate start, LocalDate end) {
        TrackerItem i = new TrackerItem();
        i.setId(id);
        i.setProject(project);
        i.setParent(parent);
        i.setTitle("Item " + id);
        i.setStatus(ItemStatus.TODO);
        i.setPosition(position);
        i.setStartDate(start);
        i.setEndDate(end);
        return i;
    }
}
