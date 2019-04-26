source('run_pathways.R')

dbms <- "postgresql"
connectionString <- "[[insert your connection string here]]"
user <- "[[insert your username here]]"
pwd <- "[[insert your password here]]"
cdmDatabaseSchema <- "[[insert your CDM database schema]]"
resultsDatabaseSchema <- "results"
cohortTable <- "cohort"
analysisId <- 1 # insert your analysis identifier

cohortDefinitions <- list({{{cohortDefinitions}}})

connectionDetails <- DatabaseConnector::createConnectionDetails(dbms = dbms,
                                                                connectionString = connectionString,
                                                                user = user,
                                                                password = pwd)

workDir = getwd()

run_pathways(workDir, connectionDetails, cdmDatabaseSchema = cdmDatabaseSchema, resultsSchema = resultsDatabaseSchema, cohortDefinitions = cohortDefinitions,
             cohortTable = cohortTable, analysisId = analysisId, outputFolder = "results")