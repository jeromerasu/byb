-- V013: Create exercise_catalog table for global exercise catalog
-- Supports admin-managed system entries and user-created custom entries

CREATE TABLE exercise_catalog (
    id                  BIGINT       PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name                VARCHAR(255) NOT NULL,
    exercise_type       VARCHAR(50),
    muscle_groups       TEXT[],
    equipment_required  TEXT[],
    difficulty_level    VARCHAR(50),
    video_url           VARCHAR(500),
    thumbnail_url       VARCHAR(500),
    instructions        TEXT,
    is_system           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by_user_id  VARCHAR(255) REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_exercise_catalog_name_user UNIQUE (name, created_by_user_id)
);

-- Indexes for common filter patterns
CREATE INDEX idx_exercise_catalog_exercise_type   ON exercise_catalog(exercise_type);
CREATE INDEX idx_exercise_catalog_is_system        ON exercise_catalog(is_system);
CREATE INDEX idx_exercise_catalog_created_by_user  ON exercise_catalog(created_by_user_id);

COMMENT ON TABLE exercise_catalog IS 'Global catalog of exercises: system entries (is_system=true) managed by admins and custom entries (is_system=false) created by individual users';
COMMENT ON COLUMN exercise_catalog.exercise_type     IS 'STRENGTH, CARDIO, FLEXIBILITY, PLYOMETRIC';
COMMENT ON COLUMN exercise_catalog.muscle_groups     IS 'Array of muscle group tags, e.g. {CHEST,TRICEPS}';
COMMENT ON COLUMN exercise_catalog.equipment_required IS 'Array of equipment tags, e.g. {BARBELL,BENCH}';
COMMENT ON COLUMN exercise_catalog.difficulty_level  IS 'BEGINNER, INTERMEDIATE, ADVANCED';
COMMENT ON COLUMN exercise_catalog.video_url         IS 'MinIO storage key for exercise video (populated separately)';
COMMENT ON COLUMN exercise_catalog.thumbnail_url     IS 'MinIO storage key for exercise thumbnail (populated separately)';
COMMENT ON COLUMN exercise_catalog.is_system         IS 'true = admin-created system entry; false = user-created custom entry';
COMMENT ON COLUMN exercise_catalog.created_by_user_id IS 'NULL for system entries; set to user ID for custom entries';
