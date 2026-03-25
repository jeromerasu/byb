-- Create single test user for consistent testing
-- This migration ensures we have exactly one test user that matches the profile user ID

-- First, delete all existing users except our test user (if it exists)
DELETE FROM users WHERE id != '3d91b1cd-aa94-48ec-b91f-edcb1e69bbbf';

-- Insert our test user if it doesn't already exist
INSERT INTO users (id, username, email, password_hash, first_name, last_name, created_at, updated_at)
VALUES (
    '3d91b1cd-aa94-48ec-b91f-edcb1e69bbbf',
    'test_consistent_user',
    'test_consistent_user@test.com',
    '$2a$10$DowJoayNM..ErwuWX3nEIuc7vfq8xB2/uFj/z0s2B.UpvqSMlNS.e', -- password123 bcrypt encoded
    'Test',
    'User',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO UPDATE SET
    username = 'test_consistent_user',
    email = 'test_consistent_user@test.com',
    updated_at = CURRENT_TIMESTAMP;

-- Also update any existing profiles to ensure they reference our test user
-- (The profiles already exist for this user ID, but this ensures consistency)
UPDATE workout_profile SET user_id = '3d91b1cd-aa94-48ec-b91f-edcb1e69bbbf'
WHERE user_id IS NOT NULL;

UPDATE diet_profile SET user_id = '3d91b1cd-aa94-48ec-b91f-edcb1e69bbbf'
WHERE user_id IS NOT NULL;