-- V034: Enrich exercise_catalog with fields sourced from the ExerciseDB API.
-- secondary_muscles: comma-delimited list of supporting muscle groups (mirrors muscle_groups storage format)
-- body_part: high-level body-part category returned by the ExerciseDB API (e.g. "back", "chest", "cardio")

ALTER TABLE exercise_catalog ADD COLUMN IF NOT EXISTS secondary_muscles TEXT;
ALTER TABLE exercise_catalog ADD COLUMN IF NOT EXISTS body_part VARCHAR(100);
