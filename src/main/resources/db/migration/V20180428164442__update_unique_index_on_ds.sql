DROP INDEX IF EXISTS datasource_name_uk;

CREATE UNIQUE INDEX datasource_name_uk
  ON datasource (name) WHERE deleted_at IS NULL;