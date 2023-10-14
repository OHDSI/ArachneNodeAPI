ALTER TABLE environment_descriptor ADD COLUMN terminated TIMESTAMP;
ALTER TABLE analyses ADD COLUMN environment_id BIGINT;
ALTER TABLE analyses ADD CONSTRAINT analyses_environment_id_fkey FOREIGN KEY (environment_id) REFERENCES environment_descriptor(id);
