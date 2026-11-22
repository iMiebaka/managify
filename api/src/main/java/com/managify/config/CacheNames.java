package com.managify.config;

/** Names of the Redis caches. Keys are stored as {@code managify::<cache>::<key>}. */
public final class CacheNames {

    private CacheNames() {}

    /** Key: project id. Value: the full tracker tree (ProjectTrackerResponse). */
    public static final String PROJECT_TRACKER = "projectTracker";

    /** Key: project id. Value: ProjectResponse. */
    public static final String PROJECT = "project";

    /** Key: worker id. Value: WorkerResponse. */
    public static final String WORKER = "worker";

    /** Key: worker id. Value: what the worker is assigned to (WorkerItemsResponse). */
    public static final String WORKER_ITEMS = "workerItems";
}
