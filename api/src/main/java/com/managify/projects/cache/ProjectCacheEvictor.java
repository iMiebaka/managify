package com.managify.projects.cache;

import com.managify.config.CacheNames;
import com.managify.projects.repository.TrackerItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;

/**
 * Evicts cache entries <b>after the transaction commits</b>, so a rolled-back write never empties the
 * cache, and a read racing the write cannot re-cache the old state between eviction and commit.
 *
 * <p>Call the methods from inside the write's transaction; they collect what to evict and publish it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectCacheEvictor {

    private final ApplicationEventPublisher publisher;

    private final CacheManager cacheManager;

    private final TrackerItemRepository items;

    public record Eviction(String cache, Object key) {}

    public record EvictionBatch(List<Eviction> evictions) {}

    /**
     * The project's tracker, plus the assignment lists of every worker assigned in the project and of
     * {@code extraWorkerIds} (e.g. a worker just unassigned, who no longer shows up in the query).
     * Call it before deleting items, so their assignees are still found.
     */
    public void trackerChanged(Long projectId, Long... extraWorkerIds) {
        List<Eviction> evictions = new ArrayList<>();
        evictions.add(new Eviction(CacheNames.PROJECT_TRACKER, projectId));

        Set<Long> workerIds = new HashSet<>(items.findAssigneeIdsInProject(projectId));
        Arrays.stream(extraWorkerIds).filter(Objects::nonNull).forEach(workerIds::add);
        workerIds.forEach(id -> evictions.add(new Eviction(CacheNames.WORKER_ITEMS, id)));

        publisher.publishEvent(new EvictionBatch(evictions));
    }

    /** The project itself and everything derived from it. */
    public void projectChanged(Long projectId) {
        evict(CacheNames.PROJECT, projectId);
        trackerChanged(projectId);
    }

    /** The worker, their assignment list and the trackers of every project they are assigned in. */
    public void workerChanged(Long workerId) {
        List<Eviction> evictions = new ArrayList<>();
        evictions.add(new Eviction(CacheNames.WORKER, workerId));
        evictions.add(new Eviction(CacheNames.WORKER_ITEMS, workerId));
        items.findProjectIdsAssignedTo(workerId)
                .forEach(projectId -> evictions.add(new Eviction(CacheNames.PROJECT_TRACKER, projectId)));
        publisher.publishEvent(new EvictionBatch(evictions));
    }

    public void evict(String cache, Object key) {
        publisher.publishEvent(new EvictionBatch(List.of(new Eviction(cache, key))));
    }

    /** Runs after commit; also runs immediately when there is no transaction. */
    @TransactionalEventListener(fallbackExecution = true)
    public void onEvictions(EvictionBatch batch) {
        for (Eviction e : batch.evictions()) {
            Cache cache = cacheManager.getCache(e.cache());
            if (cache == null) {
                continue;
            }
            try {
                cache.evict(e.key());
            } catch (RuntimeException ex) {
                // Redis down: the entry expires on its TTL instead.
                log.warn("Cache eviction failed [{}::{}]: {}", e.cache(), e.key(), ex.getMessage());
            }
        }
    }
}
