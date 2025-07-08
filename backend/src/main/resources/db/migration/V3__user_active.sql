ALTER TABLE users 
ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Set all existing users to active
UPDATE users SET active = TRUE WHERE active IS NULL;

-- Add index for better query performance
CREATE INDEX idx_users_active ON users(active);