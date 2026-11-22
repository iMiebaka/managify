-- Project management: projects, workers and the project tracker (tasks and their tasklets).
--
-- Tasks and tasklets share tracker_items: parent_id IS NULL is a task, otherwise a tasklet of
-- that task. Turning one into the other only changes parent_id and sort_order, so ids stay stable.
-- Only two levels exist; the service enforces it.

CREATE TABLE projects (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    name               VARCHAR(150)  NOT NULL,
    description        VARCHAR(2000) NULL,
    start_date         DATE          NOT NULL,
    end_date           DATE          NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    created_by         VARCHAR(100)  NULL,
    last_modified_by   VARCHAR(100)  NULL,
    date_created       DATETIME(6)   NULL,
    date_last_updated  DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_projects_name UNIQUE (name),
    INDEX idx_projects_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workers (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    full_name          VARCHAR(150)  NOT NULL,
    email              VARCHAR(200)  NOT NULL,
    keycloak_user_id   VARCHAR(64)   NULL,
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by         VARCHAR(100)  NULL,
    last_modified_by   VARCHAR(100)  NULL,
    date_created       DATETIME(6)   NULL,
    date_last_updated  DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_workers_email UNIQUE (email),
    CONSTRAINT uk_workers_keycloak_user_id UNIQUE (keycloak_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE tracker_items (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    project_id         BIGINT        NOT NULL,
    parent_id          BIGINT        NULL,
    title              VARCHAR(200)  NOT NULL,
    description        VARCHAR(2000) NULL,
    status             VARCHAR(20)   NOT NULL,
    start_date         DATE          NULL,
    end_date           DATE          NULL,
    sort_order         INT           NOT NULL,
    assignee_id        BIGINT        NULL,
    version            BIGINT        NOT NULL DEFAULT 0,
    created_by         VARCHAR(100)  NULL,
    last_modified_by   VARCHAR(100)  NULL,
    date_created       DATETIME(6)   NULL,
    date_last_updated  DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tracker_items_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_tracker_items_parent FOREIGN KEY (parent_id) REFERENCES tracker_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_tracker_items_assignee FOREIGN KEY (assignee_id) REFERENCES workers (id) ON DELETE SET NULL,
    INDEX idx_tracker_items_tree (project_id, parent_id, sort_order),
    INDEX idx_tracker_items_assignee (assignee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
