-- TASK-COACHING-001: Add nullable coach_id FK to users
-- Only set when subscription_tier = COACHING

ALTER TABLE users ADD COLUMN IF NOT EXISTS coach_id VARCHAR(36);

ALTER TABLE users ADD CONSTRAINT fk_users_coach_id
    FOREIGN KEY (coach_id) REFERENCES coaches (id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_users_coach_id ON users (coach_id);
