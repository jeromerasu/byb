-- V014: Add food_catalog_id FK column to meal_log table (nullable, no breaking changes)
ALTER TABLE meal_log
    ADD COLUMN food_catalog_id BIGINT DEFAULT NULL;

ALTER TABLE meal_log
    ADD CONSTRAINT fk_meal_log_food_catalog
        FOREIGN KEY (food_catalog_id) REFERENCES food_catalog(id) ON DELETE SET NULL;

CREATE INDEX idx_meal_log_food_catalog_id ON meal_log(food_catalog_id);
