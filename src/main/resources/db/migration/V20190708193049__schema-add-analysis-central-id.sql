CREATE TABLE IF NOT EXISTS analyses
(
	id                        BIGSERIAL
		PRIMARY KEY,
	executable_filename       VARCHAR NOT NULL,
	callback_password         VARCHAR NOT NULL,
	update_status_callback    VARCHAR NOT NULL,
	result_callback           VARCHAR NOT NULL,
	data_source_id            BIGINT  NOT NULL
		REFERENCES datasource
		ON DELETE CASCADE,
	analysis_folder           VARCHAR NOT NULL,
	stdout                    VARCHAR,
	result_status             VARCHAR,
	title                     VARCHAR(255),
	study_id                  BIGINT REFERENCES studies ON DELETE CASCADE,
	author_email              VARCHAR(100),
	author_first_name         VARCHAR(50),
	author_last_name          VARCHAR(50),
	type                      VARCHAR,
	inner_executable_filename VARCHAR
);

CREATE TABLE IF NOT EXISTS analysis_state_journal
(
	id          BIGSERIAL
		PRIMARY KEY,
	date        TIMESTAMP DEFAULT now() NOT NULL,
	state       VARCHAR                 NOT NULL,
	reason      VARCHAR                 NOT NULL,
	analysis_id BIGINT                  NOT NULL
		REFERENCES analyses
		ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS analysis_state_journal_id_seq MINVALUE 1;
ALTER SEQUENCE analysis_state_journal_id_seq OWNED BY analysis_state_journal.id;
SELECT setval('analysis_state_journal_id_seq', (SELECT MAX(id) + 1
																								FROM analysis_state_journal), FALSE);

CREATE TABLE IF NOT EXISTS analysis_files
(
	id          BIGSERIAL
		PRIMARY KEY,
	type        VARCHAR                                            NOT NULL,
	link        VARCHAR                                            NOT NULL,
	status      VARCHAR DEFAULT 'UNPROCESSED' :: CHARACTER VARYING NOT NULL,
	retries     BIGINT DEFAULT 0                                   NOT NULL,
	analysis_id BIGINT                                             NOT NULL
		REFERENCES analyses
		ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS analysis_files_id_seq MINVALUE 1;
ALTER SEQUENCE analysis_files_id_seq OWNED BY analysis_files.id;
SELECT setval('analysis_files_id_seq', (SELECT MAX(id) + 1
																				FROM analysis_files), FALSE);

CREATE TABLE IF NOT EXISTS analysis_code_files
(
	id           BIGSERIAL NOT NULL,
	uuid         VARCHAR   NOT NULL,
	name         VARCHAR   NOT NULL,
	content_type VARCHAR,
	analysis_id  BIGINT REFERENCES analyses (id) ON DELETE CASCADE ON UPDATE CASCADE
);

ALTER TABLE analyses ADD IF NOT EXISTS central_id BIGINT;

UPDATE analyses SET central_id = id WHERE analyses.central_id IS NULL;

CREATE SEQUENCE IF NOT EXISTS analyses_id_seq START WITH 5000;
SELECT setval('analyses_id_seq', (SELECT MAX(id) FROM analyses) + 1);

ALTER TABLE analysis_files DROP CONSTRAINT IF EXISTS analysis_files_analysis_id_fkey;

ALTER TABLE analysis_code_files DROP CONSTRAINT IF EXISTS analysis_code_files_analysis_id_fkey;

ALTER TABLE analysis_state_journal DROP CONSTRAINT IF EXISTS analysis_state_journal_analysis_id_fkey;

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