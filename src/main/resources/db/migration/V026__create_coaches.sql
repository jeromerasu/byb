-- TASK-COACHING-001: Create coaches table for separate coach authentication
-- Coach auth is isolated from mobile user auth (no relation to users table)

CREATE TABLE IF NOT EXISTS coaches (
    id               VARCHAR(36) PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    hashed_password  VARCHAR(255) NOT NULL,
    credentials      TEXT,
    bio              TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coaches_email ON coaches (email);
