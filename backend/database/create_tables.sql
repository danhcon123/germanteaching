-- ======================================
-- 1. Drop existing tables (in dependency order)
-- ======================================

DROP TABLE IF EXISTS users_xp_transactions;
DROP TABLE IF EXISTS user_coin_transactions;
DROP TABLE IF EXISTS user_inventory;
DROP TABLE IF EXISTS level_thresholds;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS user_preferences;
DROP TABLE IF EXISTS user_unit_challenge_progress;
DROP TABLE IF EXISTS unit_challenges;
DROP TABLE IF EXISTS user_task_attempts;
DROP TABLE IF EXISTS user_lesson_progress;
DROP TABLE IF EXISTS user_module_progress;
DROP TABLE IF EXISTS lesson_introductions;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS lessons;
DROP TABLE IF EXISTS modules;
DROP TABLE IF EXISTS users;


-- ======================================
-- 2. Optional ENUM Types (Create before use)
-- ======================================

-- It's often cleaner to create ENUM types before the tables that use them.
DROP TYPE IF EXISTS lesson_status;
DROP TYPE IF EXISTS module_status;
DROP TYPE IF EXISTS challenge_status;

CREATE TYPE lesson_status AS ENUM ('locked','unlocked','in_progress','completed');
CREATE TYPE module_status AS ENUM ('locked','unlocked','completed');
CREATE TYPE challenge_status AS ENUM ('in_progress','completed','locked');


-- ======================================
-- 3. Create main tables
-- ======================================

-- 3.1 users Table
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    xp INTEGER NOT NULL DEFAULT 0,
    lern_coins INTEGER NOT NULL DEFAULT 0,
    current_streak_days INTEGER NOT NULL DEFAULT 0,
    highest_streak_achieved INTEGER NOT NULL DEFAULT 0,
    last_streak_activity_date DATE,
    streak_freezes_owned INTEGER NOT NULL DEFAULT 0, -- Corrected 'NOT NULLL' to 'NOT NULL'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ
);

-- 3.2 modules Table
CREATE TABLE modules (
    module_id SERIAL PRIMARY KEY,
    module_title VARCHAR(255) NOT NULL,
    module_description TEXT,
    module_order INTEGER NOT NULL
);

-- 3.3 lessons Table
CREATE TABLE lessons (
    lesson_id SERIAL PRIMARY KEY,
    module_id INTEGER NOT NULL REFERENCES modules(module_id) ON DELETE CASCADE,
    lesson_title VARCHAR(150) NOT NULL,
    lesson_order INTEGER NOT NULL,
    UNIQUE(module_id, lesson_order)
);

-- 3.4 lesson_introductions Table
CREATE TABLE lesson_introductions (
    intro_id SERIAL PRIMARY KEY,
    lesson_id INTEGER NOT NULL REFERENCES lessons(lesson_id) ON DELETE CASCADE,
    word VARCHAR(100), -- e.g "Hallo"
    pronunciation VARCHAR(100),  --e.g. "['ha.lo]"
    phonetic_guide TEXT, -- Vietnamese phonetic hints
    translation VARCHAR(255), -- e.g "Xin chào"
    fact TEXT -- cultural/ fact/ funfact
);

-- 3.5 tasks Table
CREATE TABLE tasks (
    task_id SERIAL PRIMARY KEY,
    lesson_id INTEGER NOT NULL REFERENCES lessons(lesson_id) ON DELETE CASCADE,
    task_type VARCHAR(50) NOT NULL,
    task_content JSONB NOT NULL,
    task_order INTEGER NOT NULL,
    base_xp INTEGER NOT NULL DEFAULT 7,
    first_try_bonus_xp INTEGER NOT NULL DEFAULT 0,
    base_lc INTEGER NOT NULL DEFAULT 1,
    vietnamese_hint TEXT,
    UNIQUE(lesson_id, task_order)
);


-- ======================================
-- 4. User progress / activity tables
-- ======================================

-- 4.1 user_module_progress Table
CREATE TABLE user_module_progress (
    user_module_progress_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    module_id INTEGER NOT NULL REFERENCES modules(module_id) ON DELETE CASCADE,
    status module_status NOT NULL DEFAULT 'locked',
    crown_type VARCHAR(10) CHECK (crown_type IN ('gold', 'silver')) DEFAULT NULL,
    completed_at TIMESTAMPTZ,
    UNIQUE(user_id, module_id)
);

-- 4.2 user_lesson_progress Table
CREATE TABLE user_lesson_progress(
    user_lesson_progress_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    lesson_id INTEGER NOT NULL REFERENCES lessons(lesson_id) ON DELETE CASCADE,
    status lesson_status NOT NULL DEFAULT 'locked',
    stars_earned INTEGER NOT NULL DEFAULT 0 CHECK (stars_earned >= 0 AND stars_earned <= 3),
    completed_at TIMESTAMPTZ,
    last_attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    best_first_attempt_tasks_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, lesson_id)
);

-- 4.3 user_task_attempts Table
CREATE TABLE user_task_attempts (
    attempt_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    task_id INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    attempt_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    was_correct BOOLEAN NOT NULL,
    was_first_try BOOLEAN NOT NULL,
    hint_used BOOLEAN NOT NULL DEFAULT FALSE,
    xp_awarded INTEGER NOT NULL DEFAULT 0,
    lc_awarded INTEGER NOT NULL DEFAULT 0
);


-- ======================================
-- 5. Unit Challenge Tables
-- ======================================

-- 5.1 unit_challenges Table
CREATE TABLE unit_challenges (
    challenge_id SERIAL PRIMARY KEY,
    module_id INTEGER NOT NULL REFERENCES modules(module_id) ON DELETE CASCADE,
    challenge_content JSONB NOT NULL,
    base_xp INTEGER NOT NULL DEFAULT 50,
    base_lc INTEGER NOT NULL DEFAULT 5
);

-- 5.2 user_unit_challenge_progress Table
CREATE TABLE user_unit_challenge_progress (
    user_unit_challenge_progress_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    challenge_id INTEGER NOT NULL REFERENCES unit_challenges(challenge_id) ON DELETE CASCADE,
    status challenge_status NOT NULL DEFAULT 'in_progress',
    completed_at TIMESTAMPTZ,
    steps_completed INTEGER NOT NULL DEFAULT 0,
    total_steps INTEGER NOT NULL,
    xp_awarded INTEGER NOT NULL DEFAULT 0,
    lc_awarded INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, challenge_id)
);


-- ======================================
-- 6. Store / Inventory / Transactions
-- ======================================

-- 6.1 items Table (store catalog)
CREATE TABLE items (
    item_id SERIAL PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    cost_lc INTEGER NOT NULL,
    item_metadata JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 6.2 user_inventory Table (tracks owned items)
CREATE TABLE user_inventory (
    inventory_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    item_id INTEGER NOT NULL REFERENCES items(item_id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6.3 user_coin_transactions Table (LC ledger)
CREATE TABLE user_coin_transactions (
    txn_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    change_amount INTEGER NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6.4 user_xp_transaction Table (XP ledger)
CREATE TABLE user_xp_transactions (
    xp_txn_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    change_xp INTEGER NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6.5 level_thresholds Table
CREATE TABLE level_thresholds (
    player_level INTEGER PRIMARY KEY,
    level_name VARCHAR(50), -- e.g., 'Adept', 'Virtuoso'
    xp_required INTEGER NOT NULL UNIQUE,
    reward_lc INTEGER NOT NULL DEFAULT 0, -- Lern Coins awarded for reaching this level
    reward_item_id INTEGER REFERENCES items(item_id) DEFAULT NULL, -- Optional item reward
    unlock_message TEXT -- A fun message to show the user, e.g., "You've become a German Grammar Guru!"
);


-- ======================================
-- 7. User Preferences / Settings
-- ======================================
CREATE TABLE user_preferences (
    pref_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    dark_mode BOOLEAN NOT NULL DEFAULT FALSE,
    language_preference VARCHAR(10) NOT NULL DEFAULT 'en',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);


-- ======================================
-- 8. Indexes for Performance
-- ======================================

CREATE INDEX idx_user_lesson_progress_user ON user_lesson_progress(user_id);
CREATE INDEX idx_user_module_progress_user ON user_module_progress(user_id);
CREATE INDEX idx_lessons_module_order ON lessons(module_id, lesson_order);
CREATE INDEX idx_tasks_lesson_order ON tasks(lesson_id, task_order);
CREATE INDEX idx_tasks_content_gin ON tasks USING GIN (task_content);
CREATE INDEX idx_user_task_attempts_user ON user_task_attempts(user_id);
CREATE INDEX idx_user_task_attempts_task ON user_task_attempts(task_id);
CREATE INDEX idx_user_unit_challenge_prog_user ON user_unit_challenge_progress(user_id);
CREATE INDEX idx_coin_tx_user ON user_coin_transactions(user_id);
CREATE INDEX idx_xp_txn_user ON user_xp_transactions(user_id);

