-- V014: Add exercise_catalog_id FK column to workout_log
-- Nullable — no existing workout log rows are affected

ALTER TABLE workout_log
    ADD COLUMN exercise_catalog_id BIGINT DEFAULT NULL
        REFERENCES exercise_catalog(id) ON DELETE SET NULL;

CREATE INDEX idx_workout_log_exercise_catalog ON workout_log(exercise_catalog_id);

COMMENT ON COLUMN workout_log.exercise_catalog_id IS 'Optional FK to exercise_catalog; links a logged set to a catalog entry';
