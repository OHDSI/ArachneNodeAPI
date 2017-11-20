source("packratRun.r")
workDir <- getwd()
pakratPath <- "PatientLevelPredictionAnalysis.tar.gz"
packratRun(pakratPath, workDir, function(workDir) {
  dbms <- Sys.getenv("DBMS_TYPE")
  connStr <- Sys.getenv("CONNECTION_STRING")
  user <- Sys.getenv("DBMS_USERNAME")
  pw <- Sys.getenv("DBMS_PASSWORD")
  cdmDatabaseSchema <- Sys.getenv("DBMS_SCHEMA")
  cohortsDatabaseSchema <-cdmDatabaseSchema
  source("run_plp_analysis.r")
  run_plp_analysis(workDir, file.path(workDir, "analysisDescription.json"), file.path(workDir, "{{initialFileName}}"), file.path(workDir, "{{outcomeFileName}}"), dbms, connStr, user, pw, cdmDatabaseSchema, cohortsDatabaseSchema, cohortTable = "cohort", outcomeTable = "cohort")
})