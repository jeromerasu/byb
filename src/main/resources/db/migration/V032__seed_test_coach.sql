-- Seed a test coach and link to the dev user account
-- Coach password: coach123

INSERT INTO coaches (id, name, email, hashed_password, bio, created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'Test Coach',
    'coach@byb.app',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Test coach for BYB development',
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

UPDATE users
SET
    coach_id        = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    subscription_tier = 'COACHING',
    email_verified  = true
WHERE email = 'reyes.jerome@outlook.com';
