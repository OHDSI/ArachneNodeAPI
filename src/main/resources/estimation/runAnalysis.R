library(devtools)
options(devtools.install.args = "--no-multiarch")

setwd("./")
tryCatch({
    unzip('{{packageFile}}', exdir = file.path(".", "{{analysisDir}}"))
    callr::rcmd("build", c("{{analysisDir}}", c("--no-build-vignettes")), echo = TRUE, show = TRUE)
    pkg_file <- list.files(path = ".", pattern = "\\.tar\\.gz")[1]
    tryCatch({
      install.packages(pkg_file, repos = NULL, type="source", INSTALL_opts=c("--no-multiarch"))
    }, finally = {
      file.remove(pkg_file)
    })
}, finally = {
    unlink('{{analysisDir}}', recursive = TRUE, force = TRUE)
})

library(DatabaseConnector)
library({{packageName}})

tryCatch({
        maxCores <- parallel::detectCores()

        dataSourceName <- (function(name) if (name == "") "default" else name)( Sys.getenv("DATA_SOURCE_NAME") )
        dbms <- Sys.getenv("DBMS_TYPE")
        connectionString <- Sys.getenv("CONNECTION_STRING")
        user <- Sys.getenv("DBMS_USERNAME")
        pwd <- Sys.getenv("DBMS_PASSWORD")
        cdmDatabaseSchema <- Sys.getenv("DBMS_SCHEMA")
        resultsDatabaseSchema <- Sys.getenv("RESULT_SCHEMA")
        cohortsDatabaseSchema <- Sys.getenv("TARGET_SCHEMA")
        cohortTable <- Sys.getenv("COHORT_TARGET_TABLE")
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

        options(sqlRenderTempEmulationSchema = resultsDatabaseSchema)

        outputFolder <- file.path(getwd(), 'results')
        dir.create(outputFolder)

        execute(connectionDetails = connectionDetails,
                cdmDatabaseSchema = cdmDatabaseSchema,
                cohortDatabaseSchema = cohortsDatabaseSchema,
                cohortTable = cohortTable,
                oracleTempSchema = resultsDatabaseSchema,
                outputFolder = outputFolder,
                databaseId = gsub("[^-a-zA-Z0-9\\(\\)]", "_", dataSourceName),
                synthesizePositiveControls = TRUE,
                runAnalyses = TRUE,
                packageResults = TRUE,
                maxCores = maxCores,
                minCellCount = 5)
}, finally = {
        remove.packages('{{packageName}}')
})