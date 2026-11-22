package com.managify.projects.repository;

import com.managify.projects.model.TrackerItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TrackerItemRepository extends JpaRepository<TrackerItem, Long> {

    /** Every item of a project with its assignee, in one query; the tree is assembled in memory. */
    @Query("""
            select i from TrackerItem i
            left join fetch i.assignee
            where i.project.id = :projectId
            order by i.position asc, i.id asc
            """)
    List<TrackerItem> findTreeRows(@Param("projectId") Long projectId);

    Optional<TrackerItem> findByIdAndProjectId(Long id, Long projectId);

    /** The project's tasks, in order. */
    List<TrackerItem> findByProjectIdAndParentIsNullOrderByPositionAscIdAsc(Long projectId);

    /** A task's tasklets, in order. */
    List<TrackerItem> findByParentIdOrderByPositionAscIdAsc(Long parentId);

    boolean existsByParentId(Long parentId);

    /** What a worker is assigned to, across projects, with the project and parent task loaded. */
    @Query("""
            select i from TrackerItem i
            join fetch i.project
            left join fetch i.parent
            where i.assignee.id = :workerId
            order by i.project.id asc, i.position asc, i.id asc
            """)
    List<TrackerItem> findAssignedTo(@Param("workerId") Long workerId);

    @Query("select distinct i.assignee.id from TrackerItem i where i.project.id = :projectId and i.assignee is not null")
    Set<Long> findAssigneeIdsInProject(@Param("projectId") Long projectId);

    @Query("select distinct i.project.id from TrackerItem i where i.assignee.id = :workerId")
    Set<Long> findProjectIdsAssignedTo(@Param("workerId") Long workerId);
}
