DROP INDEX IF EXISTS users_username_uk;
CREATE UNIQUE INDEX IF NOT EXISTS users_username_uk ON users (lower(username));