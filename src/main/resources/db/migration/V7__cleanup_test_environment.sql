-- V7__cleanup_test_environment.sql
-- Clean up test environment to maintain only one user with complete profiles

-- First, let's identify users with both workout and diet profiles
WITH users_with_both_profiles AS (
    SELECT DISTINCT u.id as user_id, u.username, u.email
    FROM users u
    WHERE EXISTS (SELECT 1 FROM workout_profiles wp WHERE wp.user_id = u.id)
      AND EXISTS (SELECT 1 FROM diet_profiles dp WHERE dp.user_id = u.id)
),

-- Get the first user with both profiles to keep
user_to_keep AS (
    SELECT user_id, username, email
    FROM users_with_both_profiles
    ORDER BY username
    LIMIT 1
),

-- Identify users to delete (all except the one to keep)
users_to_delete AS (
    SELECT u.id as user_id
    FROM users u
    WHERE u.id NOT IN (SELECT user_id FROM user_to_keep)
)

-- Delete workout profiles for users to be deleted
DELETE FROM workout_profiles
WHERE user_id IN (SELECT user_id FROM users_to_delete);

-- Delete diet profiles for users to be deleted
DELETE FROM diet_profiles
WHERE user_id IN (SELECT user_id FROM users_to_delete);

-- Delete the users themselves
DELETE FROM users
WHERE id IN (SELECT user_id FROM users_to_delete);

-- Log the cleanup results (PostgreSQL specific)
DO $$
DECLARE
    remaining_users_count INTEGER;
    remaining_workout_profiles INTEGER;
    remaining_diet_profiles INTEGER;
    kept_user_info RECORD;
BEGIN
    SELECT COUNT(*) INTO remaining_users_count FROM users;
    SELECT COUNT(*) INTO remaining_workout_profiles FROM workout_profiles;
    SELECT COUNT(*) INTO remaining_diet_profiles FROM diet_profiles;

    -- Get info about the kept user
    SELECT u.id, u.username, u.email INTO kept_user_info
    FROM users u
    LIMIT 1;

    RAISE NOTICE 'Database cleanup completed:';
    RAISE NOTICE 'Remaining users: %', remaining_users_count;
    RAISE NOTICE 'Remaining workout profiles: %', remaining_workout_profiles;
    RAISE NOTICE 'Remaining diet profiles: %', remaining_diet_profiles;
    RAISE NOTICE 'Kept user: % (%, %)', kept_user_info.username, kept_user_info.email, kept_user_info.id;
END $$;