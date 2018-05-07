CREATE TABLE IF NOT EXISTS atlases (
  id SERIAL PRIMARY KEY,
  central_id BIGINT,
  name VARCHAR NOT NULL UNIQUE,
  version VARCHAR,
  url VARCHAR,
  auth_type VARCHAR DEFAULT 'NONE',
  username VARCHAR,
  password VARCHAR,
  cohort_log_enabled BOOLEAN DEFAULT FALSE,
  cohort_count_enabled BOOLEAN DEFAULT TRUE
);

WITH existing_atlases AS (
    SELECT
      (SELECT VALUE FROM system_settings WHERE name = 'atlas.host') AS host,
      (SELECT VALUE FROM system_settings WHERE name = 'atlas.port') AS port,
      (SELECT VALUE FROM system_settings WHERE name = 'atlas.urlContext') AS url_context,
      (SELECT VALUE FROM system_settings WHERE name = 'atlas.auth.schema') AS auth_type,
      (SELECT VALUE FROM system_settings WHERE name = 'atlas.auth.username') AS username,
      (SELECT VALUE FROM system_settings WHERE name = 'atlas.auth.password') AS password,
      (SELECT VALUE = 'TRUE' FROM system_settings WHERE name = 'cohorts.result.countEnabled') AS cohort_log_enabled,
      (SELECT VALUE = 'TRUE' FROM system_settings WHERE name = 'cohorts.result.summaryEnabled') AS cohort_count_enabled
)
INSERT INTO atlases (name, url, auth_type, username, password, cohort_log_enabled, cohort_count_enabled)
SELECT
  'Atlas' as name,
  (host || ':' || COALESCE(port, '80') || (CASE WHEN url_context IS NOT NULL THEN '/' ELSE '' END) || COALESCE(url_context, '')) AS url,
  auth_type,
  username,
  password,
  cohort_log_enabled,
  cohort_count_enabled
FROM existing_atlases;
-- WHERE host IS NOT NULL;

ALTER TABLE common_entity ADD COLUMN origin_id BIGINT REFERENCES atlases (id);
UPDATE common_entity SET origin_id = 1;
ALTER TABLE common_entity ALTER COLUMN origin_id SET NOT NULL;

