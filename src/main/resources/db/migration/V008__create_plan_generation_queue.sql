-- TASK-BE-014: Plan Generation Queue table
-- Compatible with H2 (test) and PostgreSQL (prod)
CREATE TABLE plan_generation_queue (
    id                  VARCHAR(36)   NOT NULL,
    user_id             VARCHAR(255)  NOT NULL,
    status              VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    attempt_count       INT           NOT NULL DEFAULT 0,
    max_attempts        INT           NOT NULL DEFAULT 3,
    locked_by           VARCHAR(255),
    locked_at           TIMESTAMP,
    scheduled_at        TIMESTAMP     NOT NULL,
    completed_at        TIMESTAMP,
    failed_at           TIMESTAMP,
    error_message       TEXT,
    workout_storage_key VARCHAR(1000),
    diet_storage_key    VARCHAR(1000),
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    CONSTRAINT pk_plan_generation_queue PRIMARY KEY (id)
);

CREATE INDEX idx_pgq_status          ON plan_generation_queue (status);
CREATE INDEX idx_pgq_user_id         ON plan_generation_queue (user_id);
CREATE INDEX idx_pgq_scheduled_at    ON plan_generation_queue (scheduled_at);
CREATE INDEX idx_pgq_status_attempts ON plan_generation_queue (status, attempt_count);
