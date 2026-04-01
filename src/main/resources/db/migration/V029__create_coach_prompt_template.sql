-- TASK-COACHING-001: Create coach_prompt_template table
-- user_id = null → coach's default template for all clients
-- user_id set → per-client custom template (takes precedence)

CREATE TABLE IF NOT EXISTS coach_prompt_template (
    id              VARCHAR(36) PRIMARY KEY,
    coach_id        VARCHAR(36) NOT NULL,
    user_id         VARCHAR(36),
    prompt_content  TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_coach_prompt_template_coach FOREIGN KEY (coach_id) REFERENCES coaches (id) ON DELETE CASCADE,
    CONSTRAINT fk_coach_prompt_template_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_coach_prompt_template       UNIQUE (coach_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_coach_prompt_template_coach_id ON coach_prompt_template (coach_id);
CREATE INDEX IF NOT EXISTS idx_coach_prompt_template_user_id  ON coach_prompt_template (user_id);
