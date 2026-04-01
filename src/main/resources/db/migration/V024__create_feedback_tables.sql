-- V024: Tier 1 session-level feedback tables
-- workout_feedback: captures per-session workout feedback from users
-- diet_feedback: captures per-session diet feedback from users

CREATE TABLE IF NOT EXISTS workout_feedback (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id          VARCHAR(255) NOT NULL,
    workout_date     DATE         NOT NULL,
    rating           INTEGER,
    session_comments TEXT,
    flagged_exercises TEXT,
    free_form_note   TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workout_feedback_user_date
    ON workout_feedback (user_id, workout_date);

CREATE TABLE IF NOT EXISTS diet_feedback (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id          VARCHAR(255) NOT NULL,
    feedback_date    DATE         NOT NULL,
    rating           INTEGER,
    session_comments TEXT,
    flagged_meals    TEXT,
    free_form_note   TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_diet_feedback_user_date
    ON diet_feedback (user_id, feedback_date);
