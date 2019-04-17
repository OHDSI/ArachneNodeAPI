library(devtools)

setwd("./")
tryCatch({
  unzip('{{packageFile}}', exdir = file.path(".", "{{analysisDir}}"))
  install_local(file.path(".", "{{analysisDir}}"))
}, finally = {
  unlink('{{analysisDir}}', recursive = TRUE, force = TRUE)
})

library(DatabaseConnector)
library({{packageName}})

tryCatch({
  dbms <- Sys.getenv("DBMS_TYPE")
  connectionString <- Sys.getenv("CONNECTION_STRING")
  user <- Sys.getenv("DBMS_USERNAME")
  pwd <- Sys.getenv("DBMS_PASSWORD")
  cdmDatabaseSchema <- Sys.getenv("DBMS_SCHEMA")
  resultsDatabaseSchema <- Sys.getenv("RESULT_SCHEMA")
  cohortsDatabaseSchema <- Sys.getenv("TARGET_SCHEMA")
  cohortTable <- Sys.getenv("COHORT_TARGET_TABLE")

  connectionDetails <- DatabaseConnector::createConnectionDetails(dbms = dbms,
    connectionString = connectionString,
    user = user,
    password = pwd)

  runAnalysis(connectionDetails = connectionDetails,
      cdmSchema = cdmDatabaseSchema,
      vocabularySchema = cdmDatabaseSchema,
      resultsSchema = resultsDatabaseSchema,
      cohortTable = cohortTable,
      sessionId = NULL,
      analysisId = {{analysisId}},
      outputFolder = file.path('.', 'results'))
}, finally = {
  remove.packages('{{packageName}}')
})