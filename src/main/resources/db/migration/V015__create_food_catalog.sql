-- V013: Create food_catalog table for global food/meal catalog
-- Supports admin-managed system entries (is_system=true) and user-created custom entries (is_system=false)

CREATE TABLE food_catalog (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(50),
    serving_size        VARCHAR(100),
    calories            INT,
    protein_grams       DECIMAL(8, 2),
    carbs_grams         DECIMAL(8, 2),
    fat_grams           DECIMAL(8, 2),
    fiber_grams         DECIMAL(8, 2),
    is_system           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id  VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint: same name cannot be used twice by the same user (or twice as system entry)
    CONSTRAINT uq_food_catalog_name_user UNIQUE (name, created_by_user_id),

    -- FK to users table (nullable: NULL for system entries)
    CONSTRAINT fk_food_catalog_user
        FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for common query patterns
CREATE INDEX idx_food_catalog_category       ON food_catalog(category);
CREATE INDEX idx_food_catalog_is_system      ON food_catalog(is_system);
CREATE INDEX idx_food_catalog_created_by     ON food_catalog(created_by_user_id);
CREATE INDEX idx_food_catalog_name           ON food_catalog(name);
