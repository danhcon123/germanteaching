-- ===================================================================
-- seed.sql
-- Populate Modules, Lessons, Items, Levels, Users, and All Progress Tables
-- ===================================================================


-- ======================================
-- 1. Seed Modules (12 totals)
-- ======================================
INSERT INTO modules (module_id, module_title, module_description, module_order) VALUES
  (1,  'Greetings & Introductions', 'Basic sentence structure (S-V-O), verb-second position, sein-conjugation, yes/no questions; theme: meeting people, saying hello/goodbye, introducing yourself; words: Hallo, Guten Tag, Tschüss, ich, du, er, sie, es.', 1),
  (2,  'About Me & Others',         'Present tense regular verbs (wohnen, heißen, kommen, lernen), W-questions (Wer? Woher? Wo?); theme: talking about yourself and family; words: Mutter, Vater, Bruder, Schwester, Lehrer, Student.', 2),
  (3,  'Things & Articles',         'Nouns: gender (der, die, das), definite articles, basic plural; theme: identifying objects; words: Tisch, Stuhl, Buch, Lampe, Haus, Auto, Tür, Fenster.', 3),
  (4,  'Having Things',             'Verb haben (to have), indefinite/negative articles (ein, eine, kein, keine); theme: possessions; words: Fahrrad, Handy, Tasche, Computer, Hund, Katze.', 4),
  (5,  'My Hobbies & Free Time',    'Present tense regular (spielen, hören, kochen, lesen), some irregular (essen, fahren), W-question Was?; theme: hobbies; words: spielen, lesen, kochen, schwimmen, Musik, Sport, Kino.', 5),
  (6,  'Eating & Drinking',         'Akkusativ case & articles (den, die, das, einen, eine, kein, keine), verb mögen; theme: food & drinks, ordering; words: Brot, Käse, Obst, Gemüse, Wasser, Kaffee, Tee, Milch, bestellen.', 6),
  (7,  'Abilities & Desires',       'Modal verbs können & wollen; infinitive at sentence end; theme: what you can/want; words: schwimmen, Auto fahren, reisen, lernen.', 7),
  (8,  'Daily Routine',             'Separable verbs (aufstehen, einkaufen, fernsehen), sentence structure; theme: daily schedule; words: morgens, mittags, abends.', 8),
  (9,  'Places & Directions',       'Akkusativ prepositions of direction (durch, für, gegen, ohne, um), W-question Wohin?; theme: asking/giving directions; words: Supermarkt, Park, Bahnhof, Restaurant.', 9),
  (10, 'Giving Instructions',       'Imperative (du, ihr, Sie); theme: simple commands; words: machen, gehen, kommen, sagen, öffnen, schließen.', 10),
  (11, 'Where Is It? - Location',   'Intro to dative case with location prepositions (in, an, auf, unter, über, neben), W-question Wo?; theme: describing location; words: Stuhl, Tisch, Lampe.', 11),
  (12, 'Connecting Sentences',      'Coordinating conjunctions (und, aber, oder, sondern); theme: linking ideas; review vocab.', 12);

-- ======================================
-- 2. Seed Lessons for Modules 1-3
-- ======================================
INSERT INTO lessons (lesson_id, module_id, lesson_title, lesson_order) VALUES
-- Module 1
  (  1, 1, 'Basic Greetings',             1),
  (  2, 1, 'Introducing Yourself',        2),
  (  3, 1, 'Pronouns & Personal Questions', 3),
  (  4, 1, 'Farewells & Polite Phrases',    4),
-- Module 2
  (  5, 2, 'Family & Relationships',      1),
  (  6, 2, 'Where I Live',                2),
  (  7, 2, 'Jobs & Hobbies',              3),
  (  8, 2, 'W-Questions Practice',        4),
-- Module 3
  (  9, 3, 'Definite Articles & Gender',  1),
  ( 10, 3, 'Basic Plurals',               2),
  ( 11, 3, 'Classroom & Home Vocabulary',   3),
  ( 12, 3, 'Putting It Together',         4);

-- ======================================
-- 3. Seed Items (Store Catalog)
-- ======================================
INSERT INTO items (item_id, item_name, item_type, cost_lc, item_metadata, is_active) VALUES
  (1, 'Hint Ticket Pack',   'utility',  5, '{"description":"Use to get hints on any task"}', TRUE),
  (2, 'Streak Freeze',      'utility', 10, '{"description":"Protects your streak once"}',    TRUE),
  (3, 'Avatar Outfit: Hat', 'cosmetic',20, '{"imageUrl":"/images/hat.png"}',          TRUE);

-- ======================================
-- 4. Thresholds
-- ======================================
INSERT INTO level_thresholds (player_level, level_name, xp_required, reward_lc, reward_item_id, unlock_message) VALUES
  (1, 'Beginner',     0,   0,   NULL, 'Welcome, beginner!'),
  (2, 'Explorer',   200,  10,   NULL, 'Great! You reached Level 2!'),
  (3, 'Adventurer', 400,  20,   NULL, 'Level 3 unlocked: keep it up!'),
  (4, 'Veteran',    800,  30,      3, 'Veteran status! New avatar hat unlocked.');

-- ======================================
-- 5. Seed Three Sample Users
-- ======================================
INSERT INTO users (user_id, username, email, password_hash, xp, lern_coins, current_streak_days,
   highest_streak_achieved, last_streak_activity_date, streak_freezes_owned, created_at, last_login_at) VALUES
  -- Alice signed up back in January, has kept a 3-day streak most recently
  (1,
   'alice',
   'alice@example.com',         -- real email here
   '$2a$10$hash1',               -- bcrypt hash here
   550,
   25,
   3,
   5,
   '2025-06-08',                -- last_streak_activity_date
   1,
   '2025-01-01T09:00:00Z',
   '2025-06-08T08:30:00Z'),
  -- Bob signed up in February, just started using the app
  (2,
   'bob',
   'bob@example.com',
   '$2a$10$hash2',
   150,
   5,
   1,
   2,
   '2025-06-07',
   0,
   '2025-02-01T10:15:00Z',
   '2025-06-07T09:45:00Z'),
  -- Carol joined in March and has been very active
  (3,
   'carol',
   'carol@example.com',
   '$2a$10$hash3',
   450,
   80,
   5,
   5,
   '2025-06-09',
   2,
   '2025-03-01T14:20:00Z',
   '2025-06-09T11:00:00Z');
-- ======================================
-- 6. Seed User Preferences
-- ======================================
INSERT INTO user_preferences (pref_id, user_id, email_notifications, dark_mode, language_preference, updated_at) VALUES
  (1, 1, TRUE,  FALSE, 'de', NOW()),
  (2, 2, FALSE, TRUE,  'de', NOW()),
  (3, 3, TRUE,  TRUE,  'de', NOW());

-- ======================================
-- 7. Seed Unit Challenges
-- ======================================
INSERT INTO unit_challenges (challenge_id, module_id, challenge_content, base_xp, base_lc) VALUES
  (1, 1, '{}'::JSONB, 50, 5),
  (2, 2, '{}'::JSONB, 50, 5),
  (3, 3, '{}'::JSONB, 50, 5);

-- ======================================
-- 8. Seed User Module Progress
-- ======================================
INSERT INTO user_module_progress
  (user_module_progress_id, user_id, module_id, status, crown_type, completed_at)
VALUES
  -- Alice completed Module 1 on May 20, and unlocked Module 2
  (1, 1, 1, 'completed', 'gold',   '2025-05-20T12:00:00Z'),
  (2, 1, 2, 'unlocked',  NULL,      NULL),
  -- Bob has just unlocked Module 1 today
  (3, 2, 1, 'unlocked',  NULL,      NULL),
  -- Carol completed Module 1 back in April, earned silver
  (4, 3, 1, 'completed', 'silver', '2025-04-15T11:00:00Z');

-- ----------------------------------------
-- 9. Seed User Lesson Progress
-- ----------------------------------------
INSERT INTO user_lesson_progress
  (user_lesson_progress_id, user_id, lesson_id, status, stars_earned, completed_at, last_attempted_at, best_first_attempt_tasks_count)
VALUES
  -- Alice cleared Lesson 1 on May 18, then Lesson 2 on May 19
  (1, 1,  1, 'completed',   3, '2025-05-18T10:00:00Z', '2025-05-18T10:00:00Z', 5),
  (2, 1,  2, 'completed',   2, '2025-05-19T11:00:00Z', '2025-05-19T11:00:00Z', 4),
  -- Bob just attempted Lesson 1 yesterday
  (3, 2,  1, 'in_progress', 1, NULL,                   '2025-06-07T09:00:00Z', 2),
  -- Carol cleared Lesson 1 on April 15
  (4, 3,  1, 'completed',   2, '2025-04-15T11:30:00Z', '2025-04-15T11:30:00Z', 3);

-- ----------------------------------------
-- 10. Seed Tasks (CRITICAL: THIS SECTION WAS MISSING)
-- This is a placeholder. You must add actual tasks for the user_task_attempts to work.
-- ----------------------------------------
INSERT INTO tasks(task_id, lesson_id, task_type, task_content, task_order) VALUES
(101, 1, 'multiple_choice', '{"question": "What is Hello in German?", "options": ["Hallo", "Tschüss", "Danke"], "answer": "Hallo"}'::JSONB, 1),
(102, 1, 'fill_in_the_blank', '{"prompt": "Ich ___ Bob.", "answer": "bin"}'::JSONB, 2);
-- Add more tasks for other lessons as needed


-- ----------------------------------------
-- 11. Seed User Task Attempts
-- ----------------------------------------
INSERT INTO user_task_attempts (attempt_id, user_id, task_id, attempt_timestamp, was_correct, was_first_try, hint_used, xp_awarded, lc_awarded) VALUES
  (1, 1, 101, '2025-05-18T09:50:00Z', TRUE,  TRUE,  FALSE, 12, 2),
  (2, 1, 101, '2025-05-18T09:55:00Z', TRUE,  FALSE, TRUE,   7, -1),
  (3, 1, 102, '2025-05-19T10:10:00Z', TRUE,  TRUE,  FALSE, 12, 2),
  (4, 2, 101, '2025-06-07T08:50:00Z', FALSE, TRUE,  FALSE,  0, 0);

-- ----------------------------------------
-- 12. Seed User Unit Challenge Progress
-- ----------------------------------------
INSERT INTO user_unit_challenge_progress (user_unit_challenge_progress_id, user_id, challenge_id, status, completed_at, steps_completed, total_steps, xp_awarded, lc_awarded) VALUES
  (1, 1, 1, 'completed',   '2025-05-20T12:30:00Z', 5, 5, 120, 20),
  (2, 2, 1, 'in_progress', NULL,                   2, 5,   0,  0),
  (3, 3, 1, 'completed',   '2025-04-15T12:00:00Z', 5, 5, 100, 10);

-- ----------------------------------------
-- 13. Seed User Inventory
-- ----------------------------------------
INSERT INTO user_inventory (inventory_id, user_id, item_id, quantity, purchased_at) VALUES
  (1, 1, 1, 2, '2025-05-18T09:00:00Z'),
  (2, 1, 2, 1, '2025-05-19T09:30:00Z'),
  (3, 2, 1, 1, '2025-06-07T08:30:00Z');

-- ----------------------------------------
-- 14. Seed User Coin Transactions (LC ledger)
-- ----------------------------------------
INSERT INTO user_coin_transactions (txn_id, user_id, change_amount, reason, created_at) VALUES
  (1, 1,  +50, 'signup_bonus',      '2025-01-01T08:00:00Z'),
  (2, 1,  +20, 'lesson_completion', '2025-05-18T10:00:00Z'),
  (3, 1,  +20, 'lesson_completion', '2025-05-19T11:00:00Z'),
  (4, 1,  -10, 'streak_freeze',     '2025-05-19T09:30:00Z'),
  (5, 2,  +50, 'signup_bonus',      '2025-02-15T09:30:00Z');

-- ----------------------------------------
-- 15. Seed User XP Transactions (XP ledger)
-- ----------------------------------------
INSERT INTO user_xp_transactions (xp_txn_id, user_id, change_xp, reason, created_at) VALUES
  (1, 1, +100, 'signup_bonus',      '2025-01-01T08:00:00Z'),
  (2, 1, +150, 'lesson_completion', '2025-05-18T10:00:00Z'),
  (3, 1, +140, 'lesson_completion', '2025-05-19T11:00:00Z'),
  (4, 1, +120, 'unit_challenge',    '2025-05-20T12:30:00Z'),
  (5, 2, +100, 'signup_bonus',      '2025-02-15T09:30:00Z');

-- ===================================================================
-- End of seed.sql
-- ===================================================================
