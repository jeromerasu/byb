-- V012: Add feedback fields to workout_log and meal_log tables
-- WorkoutLog feedback: rating, comment, pain flag, substitution request
ALTER TABLE workout_log
    ADD COLUMN rating              VARCHAR(20)  DEFAULT NULL,
    ADD COLUMN feedback_comment    TEXT         DEFAULT NULL,
    ADD COLUMN pain_flag           BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN substitution_requested BOOLEAN   NOT NULL DEFAULT FALSE;

-- MealLog feedback: rating and comment
ALTER TABLE meal_log
    ADD COLUMN rating           VARCHAR(20) DEFAULT NULL,
    ADD COLUMN feedback_comment TEXT        DEFAULT NULL;
