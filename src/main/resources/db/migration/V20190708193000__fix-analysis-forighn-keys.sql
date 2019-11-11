-- Remove unnecessary column constants
-- There is no rename if exists/add if not exists operation in postgres, so I put a combination of drop/add operation
-- at the end only analysis_files_analysis_id_fkey, analysis_code_files_analysis_id_fkey, analysis_state_journal_analysis_id_fkey constrains are going to remain

ALTER TABLE analysis_files DROP CONSTRAINT IF EXISTS fk_analysis_id;
ALTER TABLE analysis_code_files DROP CONSTRAINT IF EXISTS fk_analysis_id;
ALTER TABLE analysis_state_journal DROP CONSTRAINT IF EXISTS fk_analysis_id;

ALTER TABLE analysis_files DROP CONSTRAINT IF EXISTS analysis_files_analysis_id_fkey;
ALTER TABLE analysis_code_files DROP CONSTRAINT IF EXISTS analysis_code_files_analysis_id_fkey;
ALTER TABLE analysis_state_journal DROP CONSTRAINT IF EXISTS analysis_state_journal_analysis_id_fkey;

ALTER TABLE analysis_files ADD CONSTRAINT analysis_files_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES analyses(id);
ALTER TABLE analysis_code_files ADD CONSTRAINT analysis_code_files_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES analyses(id);
ALTER TABLE analysis_state_journal ADD CONSTRAINT analysis_state_journal_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES analyses(id);
