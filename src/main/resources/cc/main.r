source("run_cc_reports.R")
workDir <- getwd()

dbms <- Sys.getenv("DBMS_TYPE")
connStr <- Sys.getenv("CONNECTION_STRING")
user <- Sys.getenv("DBMS_USERNAME")
pw <- Sys.getenv("DBMS_PASSWORD")
cdmDatabaseSchema <- Sys.getenv("DBMS_SCHEMA")  # may be null if not defined in datasource's metadata
resultsDatabaseSchema <- Sys.getenv("RESULT_SCHEMA")  # may be null if not defined in datasource's metadata

run_cohort_characterization(cohortDefinitionSqlPath = file.path(workDir, "{{initialFileName}}"),
                            outputFolder = file.path(workDir, "output"),
                            dbms = dbms,
                            connectionString = connStr,
                            user = user,
                            password = pw,
                            cdmDatabaseSchema = cdmDatabaseSchema,
                            resultsDatabaseSchema = resultsDatabaseSchema,
                            cohortTable = "cohort",
                            includeDrilldownReports = FALSE,
                            includedReports = list("cohortSpecific" = TRUE,
                                                   "death" = TRUE,
                                                   "cohortObservationPeriod" = TRUE,
                                                   "person" = FALSE,
                                                   "dataCompleteness" = TRUE,
                                                   "dashboard" = TRUE,
                                                   "heraclesHeel" = TRUE,
                                                   "entropy" = TRUE,
                                                   "conditionTreemap" = TRUE,
                                                   "drugEraTreemap" = TRUE,
                                                   "drugExposuresTreemap" = TRUE,
                                                   "procedureTreemap" = TRUE,
                                                   "visitTreemap" = TRUE,
                                                   "conditionEraTreemap" = TRUE,
                                                   "conditionsByIndex" = TRUE,
                                                   "proceduresByIndex" = TRUE,
                                                   "drugsByIndex" = TRUE),
                            cohortResults = list("count" = "{{countEnabled}}",
                                                 "summary" = "{{summaryEnabled}}")
)