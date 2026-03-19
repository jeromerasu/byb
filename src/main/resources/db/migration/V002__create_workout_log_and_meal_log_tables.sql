-- Create workout_log table for tracking workout exercises
CREATE TABLE workout_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    exercise VARCHAR(255) NOT NULL,
    weight DECIMAL(5,2),
    date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint to users table
    CONSTRAINT fk_workout_log_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create meal_log table for tracking meal consumption
CREATE TABLE meal_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    meal_name VARCHAR(255) NOT NULL,
    calories DECIMAL(7,2),
    proteins DECIMAL(6,2),
    fats DECIMAL(6,2),
    carbs DECIMAL(6,2),
    date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint to users table
    CONSTRAINT fk_meal_log_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_workout_log_user_id ON workout_log(user_id);
CREATE INDEX idx_workout_log_date ON workout_log(date);
CREATE INDEX idx_workout_log_user_date ON workout_log(user_id, date);

CREATE INDEX idx_meal_log_user_id ON meal_log(user_id);
CREATE INDEX idx_meal_log_date ON meal_log(date);
CREATE INDEX idx_meal_log_user_date ON meal_log(user_id, date);

-- Add comments for documentation
COMMENT ON TABLE workout_log IS 'Tracks individual workout exercise logs for users';
COMMENT ON COLUMN workout_log.user_id IS 'References the user ID from the users table';
COMMENT ON COLUMN workout_log.exercise IS 'Name or description of the exercise performed';
COMMENT ON COLUMN workout_log.weight IS 'Weight used for the exercise in kilograms';
COMMENT ON COLUMN workout_log.date IS 'Date when the workout was performed';

COMMENT ON TABLE meal_log IS 'Tracks individual meal consumption logs for users';
COMMENT ON COLUMN meal_log.user_id IS 'References the user ID from the users table';
COMMENT ON COLUMN meal_log.meal_name IS 'Name or description of the meal consumed';
COMMENT ON COLUMN meal_log.calories IS 'Total calories in the meal';
COMMENT ON COLUMN meal_log.proteins IS 'Protein content in grams';
COMMENT ON COLUMN meal_log.fats IS 'Fat content in grams';
COMMENT ON COLUMN meal_log.carbs IS 'Carbohydrate content in grams';
COMMENT ON COLUMN meal_log.date IS 'Date when the meal was consumed';