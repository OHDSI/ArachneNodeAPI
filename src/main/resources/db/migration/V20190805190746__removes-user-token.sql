ALTER TABLE users DROP COLUMN IF EXISTS token;

CREATE SEQUENCE IF NOT EXISTS user_id_seq;

SELECT setval('user_id_seq', (SELECT MAX(id)+1 FROM users));

ALTER TABLE users DROP CONSTRAINT IF EXISTS user_email_key;

CREATE UNIQUE INDEX IF NOT EXISTS users_username_uk ON users(username);