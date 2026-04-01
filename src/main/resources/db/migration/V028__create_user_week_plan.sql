-- TASK-COACHING-001: Create user_week_plan registry (closes 016C gap)
-- Tracks which storage keys correspond to which user+week combination

CREATE TABLE IF NOT EXISTS user_week_plan (
    id                   VARCHAR(36) PRIMARY KEY,
    user_id              VARCHAR(36) NOT NULL,
    week_start           DATE NOT NULL,
    workout_storage_key  VARCHAR(1000),
    diet_storage_key     VARCHAR(1000),
    generated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    generated_by         VARCHAR(50) NOT NULL DEFAULT 'CRON',
    CONSTRAINT fk_user_week_plan_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_week_plan_user_week UNIQUE (user_id, week_start)
);

CREATE INDEX IF NOT EXISTS idx_user_week_plan_user_id ON user_week_plan (user_id);
CREATE INDEX IF NOT EXISTS idx_user_week_plan_week_start ON user_week_plan (week_start);
