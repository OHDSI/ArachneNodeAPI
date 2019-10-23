setwd("./")
libs_local <- file.path(getwd(), "libs-local")
tryCatch({
  unzip('{{packageFile}}', exdir = file.path(".", "{{analysisDir}}"))
  dir.create(libs_local)
  callr::rcmd("build", c("{{analysisDir}}", c("--no-build-vignettes")), echo = TRUE, show = TRUE)
  pkg_file <- list.files(path = ".", pattern = "\\.tar\\.gz")[1]
  tryCatch({
    install.packages(pkg_file, lib = libs_local, repos = NULL, type="source", INSTALL_opts=c("--no-multiarch"))
  }, finally = {
    file.remove(pkg_file)
  })
}, finally = {
  unlink('{{analysisDir}}', recursive = TRUE, force = TRUE)
})

.libPaths(c(.libPaths(), libs_local))

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
  driversPath <- (function(path) if (path == "") NULL else path)( Sys.getenv("JDBC_DRIVER_PATH") )
  analysisId <- (function(id) if (id == "") {{analysisId}} else id)( Sys.getenv("ANALYSIS_ID") )

  outputFolder <- file.path(getwd(), 'results')
  dir.create(outputFolder)

  connectionDetails <- DatabaseConnector::createConnectionDetails(dbms = dbms,
    connectionString = connectionString,
    user = user,
    password = pwd,
    pathToDriver = driversPath)

  cohortTable <- createCohorts(
    pckg = "{{packageName}}",
    connectionDetails = connectionDetails,
    cdmSchema = cdmDatabaseSchema,
    vocabularySchema = cdmDatabaseSchema,
    resultSchema = resultsDatabaseSchema,
    tempSchema = resultsDatabaseSchema
  )

  runAnalysis(connectionDetails = connectionDetails,
      cdmSchema = cdmDatabaseSchema,
      vocabularySchema = cdmDatabaseSchema,
      resultsSchema = resultsDatabaseSchema,
      cohortTable = cohortTable,
      sessionId = NULL,
      analysisId = analysisId,
      outputFolder = outputFolder)

  cleanupCohortTable(
    connectionDetails = connectionDetails,
    resultSchema = resultsDatabaseSchema,
    cohortTable = cohortTable,
    tempSchema = resultsDatabaseSchema
  )
}, finally = {
  remove.packages('{{packageName}}', lib = libs_local)
  unlink(libs_local, recursive = TRUE, force = TRUE)
})