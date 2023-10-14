library(Strategus)

json <- paste(readLines(file("@studyJson")), collapse = '\n')
analysisSpecifications <- ParallelLogger::convertJsonToSettings(json)

dbms <- Sys.getenv("DBMS_TYPE")
connectionString <- Sys.getenv("CONNECTION_STRING")
user <- Sys.getenv("DBMS_USERNAME")
pwd <- Sys.getenv("DBMS_PASSWORD")
cdmDatabaseSchema <- Sys.getenv("DBMS_SCHEMA")
workDatabaseSchema <- Sys.getenv("RESULT_SCHEMA")
cohortsDatabaseSchema <- Sys.getenv("TARGET_SCHEMA")
cohortTableName <- Sys.getenv("COHORT_TARGET_TABLE")
minCellCount <- 5
driversPath <- (function(path) if (path == "") NULL else path)( Sys.getenv("JDBC_DRIVER_PATH") )

connectionDetails <- DatabaseConnector::createConnectionDetails(dbms = dbms,
                                                                connectionString = connectionString,
                                                                user = user,
                                                                password = pwd,
                                                                pathToDriver = driversPath)

# Evaluating can't use global environment in child threads
connectionDetails$user <- function() Sys.getenv("DBMS_USERNAME")
connectionDetails$password <- function() Sys.getenv("DBMS_PASSWORD")
connectionDetails$connectionString <- function() Sys.getenv("CONNECTION_STRING")

connectionDetailsReference <- Sys.getenv("DATA_SOURCE_NAME")

outputLocation <- '/results'

storeConnectionDetails(
  connectionDetails = connectionDetails,
  connectionDetailsReference = connectionDetailsReference
)

executionSettings <- createCdmExecutionSettings(
  connectionDetailsReference = connectionDetailsReference,
  workDatabaseSchema = workDatabaseSchema,
  cdmDatabaseSchema = cdmDatabaseSchema,
  cohortTableNames = CohortGenerator::getCohortTableNames(cohortTable = cohortTableName),
  workFolder = file.path(outputLocation, "strategusWork"),
  resultsFolder = file.path(outputLocation, "strategusOutput"),
  minCellCount = minCellCount
)

execute(
  analysisSpecifications = analysisSpecifications,
  executionSettings = executionSettings,
  executionScriptFolder = file.path(outputLocation, "strategusExecution")
)