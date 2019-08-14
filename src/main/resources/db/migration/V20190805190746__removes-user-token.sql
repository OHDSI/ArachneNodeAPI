CREATE SEQUENCE IF NOT EXISTS user_id_seq;

SELECT setval('user_id_seq', (SELECT MAX(id)+1 FROM users));

ALTER TABLE users DROP CONSTRAINT IF EXISTS user_email_key;

CREATE UNIQUE INDEX IF NOT EXISTS users_username_uk ON users(username);

ALTER TABLE users DROP IF EXISTS token;
ALTER TABLE users ADD IF NOT EXISTS is_sync BOOLEAN DEFAULT FALSE;

-- No stand-alone mode before and therefore all existing users are synchronized
UPDATE users SET is_sync = TRUE;