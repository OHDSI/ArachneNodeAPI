ALTER TABLE analyses ADD IF NOT EXISTS central_id BIGINT;

UPDATE analyses SET central_id = id WHERE analyses.central_id IS NULL;

CREATE SEQUENCE IF NOT EXISTS analyses_id_seq START WITH 5000;
SELECT setval('analyses_id_seq', (SELECT MAX(id) FROM analyses) + 1);

ALTER TABLE analysis_files DROP CONSTRAINT analysis_files_analysis_id_fkey;

ALTER TABLE analysis_code_files DROP CONSTRAINT analysis_code_files_analysis_id_fkey;

ALTER TABLE analysis_state_journal DROP CONSTRAINT analysis_state_journal_analysis_id_fkey;

UPDATE analyses SET id = nextval('analyses_id_seq');

UPDATE analysis_files SET analysis_id = a.id
FROM analyses a WHERE a.central_id = analysis_id;

UPDATE analysis_code_files SET analysis_id = a.id
FROM analyses a WHERE a.central_id = analysis_id;

UPDATE analysis_state_journal SET analysis_id = a.id
FROM analyses a WHERE a.central_id = analysis_id;

ALTER TABLE analysis_files ADD CONSTRAINT analysis_files_analysis_id_fkey
	FOREIGN KEY (analysis_id) REFERENCES analyses(id);

ALTER TABLE analysis_code_files ADD CONSTRAINT analysis_code_files_analysis_id_fkey
	FOREIGN KEY (analysis_id) REFERENCES analyses(id);

ALTER TABLE analysis_state_journal ADD CONSTRAINT analysis_state_journal_analysis_id_fkey
	FOREIGN KEY (analysis_id) REFERENCES analyses(id);