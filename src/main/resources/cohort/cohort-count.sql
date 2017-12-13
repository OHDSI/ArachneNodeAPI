SELECT count(*) count FROM @target_database_schema.@target_cohort_table
WHERE cohort_definition_id = @target_cohort_id;