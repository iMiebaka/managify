package com.managify.projects.model;

import com.managify.model.common.generic.FullyAuditableEntity;
import com.managify.projects.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** A project: a name, a duration (start to end date) and a tracker of tasks. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "projects")
public class Project extends FullyAuditableEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;
}
