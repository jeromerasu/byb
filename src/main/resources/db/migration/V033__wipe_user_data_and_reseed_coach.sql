-- V033: One-time data wipe for clean onboarding tests
-- Preserves: exercise_catalog, food_catalog, flyway_schema_history
-- Wipes: all user-generated data (users, profiles, logs, feedback, plans, coaches, directives, billing)
-- Re-seeds: test coach (coach@byb.app / coach123)

TRUNCATE TABLE
    coach_directive,
    coach_prompt_template,
    billing_entitlements,
    webhook_event_log,
    plan_usage_tracker,
    user_week_plan,
    workout_log,
    meal_log,
    body_metrics,
    plan_generation_queue,
    workout_feedback,
    diet_feedback,
    workout_profile,
    diet_profile,
    users,
    coaches
CASCADE;

-- Re-seed test coach: coach@byb.app / coach123
INSERT INTO coaches (id, name, email, hashed_password, bio, created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'Test Coach',
    'coach@byb.app',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Test coach for BYB development',
    NOW(),
    NOW()
);
