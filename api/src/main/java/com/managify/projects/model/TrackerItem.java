package com.managify.projects.model;

import com.managify.model.common.generic.FullyAuditableEntity;
import com.managify.projects.enums.ItemStatus;
import com.managify.projects.enums.ItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * One row of a project tracker. With no parent it is a <b>task</b>; with a parent (always a task of
 * the same project) it is a <b>tasklet</b>. {@code position} orders items among their siblings.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tracker_items")
public class TrackerItem extends FullyAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TrackerItem parent;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(name = "sort_order", nullable = false)
    private int position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Worker assignee;

    @Version
    private long version;

    public boolean isTask() {
        return parent == null;
    }

    public ItemType getType() {
        return isTask() ? ItemType.TASK : ItemType.TASKLET;
    }
}
