library(jsonlite)
library(dplyr)
library(DatabaseConnector)
library(SqlRender)
source("run_heracles_analysis.r")

workDir <- getwd()
unzip('cohortresults_pack.zip', exdir = file.path("."), overwrite = T)

dbms <- Sys.getenv("DBMS_TYPE")
connStr <- Sys.getenv("CONNECTION_STRING")
user <- Sys.getenv("DBMS_USERNAME")
pw <- Sys.getenv("DBMS_PASSWORD")
cdmDatabaseSchema <- Sys.getenv("DBMS_SCHEMA")  # may be null if not defined in datasource's metadata
resultsDatabaseSchema <- Sys.getenv("RESULT_SCHEMA")  # may be null if not defined in datasource's metadata



if ("impala" == dbms) {
  driverPath <- Sys.getenv("IMPALA_DRIVER_PATH")
  if (missing(driverPath) || is.null(driverPath) || driverPath == ''){
    driverPath <- "/impala"
  }
  connectionDetails <- createConnectionDetails(dbms=dbms,
                                               connectionString=connStr,
                                               user=user,
                                               password=pw,
                                               pathToDriver=driverPath)
} else {
  connectionDetails <- createConnectionDetails(dbms=dbms,
                                               connectionString=connStr,
                                               user=user,
                                               password=pw)
}
print(connectionDetails)

start.time <- Sys.time()
cohortDefinitionSqlPath <- file.path(workDir, "{{initialFileName}}")
cohortId <- run_cohort_characterization(cohortDefinitionSqlPath = cohortDefinitionSqlPath,
                                        connectionDetails,
                                        cdmDatabaseSchema = cdmDatabaseSchema,
                                        resultsDatabaseSchema = resultsDatabaseSchema,
                                        cohortTable = "cohort"
)

exportResults(
  cohortId = cohortId,
  outputFolder = file.path(workDir, "output"),
  connectionDetails = connectionDetails,
  cdmDatabaseSchema = cdmDatabaseSchema,
  resultsDatabaseSchema = resultsDatabaseSchema,
  cohortTable = "cohort",
  cohortDefinitionSqlPath = cohortDefinitionSqlPath,
  includeDrilldownReports = T,
  includedReports = list("cohortSpecific" = TRUE,
                         "death" = TRUE,
                         "cohortObservationPeriod" = TRUE,
                         "person" = TRUE,
                         "dataCompleteness" = TRUE,
                         "dashboard" = TRUE,
                         "heraclesHeel" = FALSE,
                         "entropy" = TRUE,
                         "conditionTreemap" = TRUE,
                         "drugEraTreemap" = TRUE,
                         "drugExposuresTreemap" = TRUE,
                         "procedureTreemap" = TRUE,
                         "visitTreemap" = TRUE,
                         "conditionEraTreemap" = TRUE,
                         "conditionsByIndex" = TRUE,
                         "proceduresByIndex" = TRUE,
                         "drugsByIndex" = TRUE)
)

print(paste("Total Execution Time ", Sys.time() -start.time, sep = " : "))

unlink('cohortresults-sql', recursive = TRUE, force = TRUE)
