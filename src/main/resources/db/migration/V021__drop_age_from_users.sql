-- Age is now derived at runtime from date_of_birth; drop the stored age column
ALTER TABLE users DROP COLUMN IF EXISTS age;
