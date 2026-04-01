-- TASK-COACHING-001: Add subscription_tier column to users table
-- Default to FREE for all existing users

ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_tier VARCHAR(50) NOT NULL DEFAULT 'FREE';
