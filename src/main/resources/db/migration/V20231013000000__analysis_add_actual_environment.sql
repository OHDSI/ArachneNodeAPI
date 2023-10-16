ALTER TABLE analyses ADD COLUMN actual_environment_id BIGINT;
ALTER TABLE analyses ADD CONSTRAINT analyses_actual_environment_id_fkey FOREIGN KEY (environment_id) REFERENCES environment_descriptor(id);
