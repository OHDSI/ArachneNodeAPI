setwd("./")
tryCatch({
  unzip('{{packageFile}}', exdir = file.path(".", "{{analysisDir}}"))
  callr::rcmd("build", c("{{analysisDir}}", c("--no-build-vignettes")), echo = TRUE, show = TRUE)
  install.packages(list.files(path = ".", pattern = "\\.tar\\.gz")[1], repo=NULL, type="source", INSTALL_opts=c("--no-multiarch"))
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
  driversPath <- (function(path) if (path == "") NULL else path)( Sys.getenv("JDBC_DRIVER_PATH") )

  outputFolder <- file.path(getwd(), 'results')
  dir.create(outputFolder)

  connectionDetails <- DatabaseConnector::createConnectionDetails(dbms = dbms,
    connectionString = connectionString,
    user = user,
    password = pwd,
    pathToDriver = driversPath)

  runAnalysis(connectionDetails = connectionDetails,
      cdmSchema = cdmDatabaseSchema,
      vocabularySchema = cdmDatabaseSchema,
      resultsSchema = resultsDatabaseSchema,
      cohortTable = cohortTable,
      sessionId = NULL,
      analysisId = {{analysisId}},
      outputFolder = outputFolder)
}, finally = {
  remove.packages('{{packageName}}')
})