ALTER TABLE datasource
  ADD COLUMN cohort_target_schema VARCHAR,
  ADD COLUMN cohort_result_schema VARCHAR,
  ADD COLUMN cohort_target_table VARCHAR;

DELETE FROM system_settings
WHERE name IN ('cohorts.target.dbSchema',
               'cohorts.result.dbSchema',
               'cohorts.target.cohortTable');