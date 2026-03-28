-- TASK: Enhance workout_log with exercise tracking fields (sets, reps, duration, type, notes)
ALTER TABLE workout_log
    ADD COLUMN sets INT,
    ADD COLUMN reps INT,
    ADD COLUMN duration_minutes INT,
    ADD COLUMN exercise_type VARCHAR(50),
    ADD COLUMN notes TEXT;

-- Add index on exercise_type for PR/analytics queries
CREATE INDEX idx_workout_log_exercise ON workout_log(exercise);
CREATE INDEX idx_workout_log_user_exercise ON workout_log(user_id, exercise);

COMMENT ON COLUMN workout_log.sets IS 'Number of sets performed';
COMMENT ON COLUMN workout_log.reps IS 'Number of reps per set';
COMMENT ON COLUMN workout_log.duration_minutes IS 'Duration in minutes (primarily for cardio exercises)';
COMMENT ON COLUMN workout_log.exercise_type IS 'Type of exercise: STRENGTH, CARDIO, or FLEXIBILITY';
COMMENT ON COLUMN workout_log.notes IS 'Optional free-text notes about the exercise session';
