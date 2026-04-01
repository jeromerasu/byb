-- TASK-COACHING-001: Create coach_directive table
-- Active directives are injected into the prompt at generation time

CREATE TABLE IF NOT EXISTS coach_directive (
    id              VARCHAR(36) PRIMARY KEY,
    coach_id        VARCHAR(36) NOT NULL,
    user_id         VARCHAR(36) NOT NULL,
    directive_type  VARCHAR(50) NOT NULL,
    content         TEXT NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_coach_directive_coach FOREIGN KEY (coach_id) REFERENCES coaches (id) ON DELETE CASCADE,
    CONSTRAINT fk_coach_directive_user  FOREIGN KEY (user_id)  REFERENCES users (id)   ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_coach_directive_coach_id ON coach_directive (coach_id);
CREATE INDEX IF NOT EXISTS idx_coach_directive_user_id  ON coach_directive (user_id);
CREATE INDEX IF NOT EXISTS idx_coach_directive_active   ON coach_directive (user_id, active);
