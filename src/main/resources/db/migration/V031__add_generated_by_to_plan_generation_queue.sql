-- TASK-COACHING-001: Add generated_by column to plan_generation_queue (closes 016A gap)
-- Nullable to avoid migration-time data loss on existing rows; defaults to MANUAL

ALTER TABLE plan_generation_queue ADD COLUMN IF NOT EXISTS generated_by VARCHAR(50) DEFAULT 'MANUAL';
