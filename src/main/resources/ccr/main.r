workDir <- getwd()
dbms <- Sys.getenv("DBMS_TYPE")
connStr <- Sys.getenv("CONNECTION_STRING")
user <- Sys.getenv("DBMS_USERNAME")
pw <- Sys.getenv("DBMS_PASSWORD")
cdmDatabaseSchema <- Sys.getenv("DBMS_SCHEMA")
resultsDatabaseSchema <- "results"

source("run_cc_reports.r")
run_cohort_characterization(file.path(workDir, "{{initialFileName}}"), file.path(workDir, "output"), dbms, connStr, user, pw, cdmDatabaseSchema, resultsDatabaseSchema)
