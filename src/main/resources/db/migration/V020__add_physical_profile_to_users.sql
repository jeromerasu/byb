-- Add physical profile columns to users table
ALTER TABLE users ADD COLUMN height_cm INTEGER;
ALTER TABLE users ADD COLUMN weight_kg DECIMAL(5,2);
ALTER TABLE users ADD COLUMN age INTEGER;
ALTER TABLE users ADD COLUMN gender VARCHAR(30);
ALTER TABLE users ADD COLUMN activity_level VARCHAR(30);

-- Migrate existing data from workout_profile to users (prefer workout_profile as source)
UPDATE users SET
    height_cm = (SELECT wp.height_cm FROM workout_profile wp WHERE wp.user_id = users.id AND wp.height_cm IS NOT NULL LIMIT 1),
    weight_kg = (SELECT wp.weight_kg FROM workout_profile wp WHERE wp.user_id = users.id AND wp.weight_kg IS NOT NULL LIMIT 1),
    age = (SELECT wp.age FROM workout_profile wp WHERE wp.user_id = users.id AND wp.age IS NOT NULL LIMIT 1),
    gender = (SELECT wp.gender FROM workout_profile wp WHERE wp.user_id = users.id AND wp.gender IS NOT NULL LIMIT 1),
    activity_level = (SELECT wp.activity_level FROM workout_profile wp WHERE wp.user_id = users.id AND wp.activity_level IS NOT NULL LIMIT 1)
WHERE EXISTS (SELECT 1 FROM workout_profile wp WHERE wp.user_id = users.id);

-- Drop physical columns from workout_profile
ALTER TABLE workout_profile DROP COLUMN height_cm;
ALTER TABLE workout_profile DROP COLUMN weight_kg;
ALTER TABLE workout_profile DROP COLUMN age;
ALTER TABLE workout_profile DROP COLUMN gender;
ALTER TABLE workout_profile DROP COLUMN activity_level;

-- Drop physical columns from diet_profile
ALTER TABLE diet_profile DROP COLUMN height_cm;
ALTER TABLE diet_profile DROP COLUMN weight_kg;
ALTER TABLE diet_profile DROP COLUMN age;
ALTER TABLE diet_profile DROP COLUMN gender;
ALTER TABLE diet_profile DROP COLUMN activity_level;
