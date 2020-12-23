library(devtools)
options(devtools.install.args = "--no-multiarch")

setwd("./")
tryCatch({
    unzip('{{packageFile}}', exdir = file.path(".", "{{analysisDir}}"))
    install.packages(file.path(".", "{{analysisDir}}"), repos = NULL, type = "source", INSTALL_opts=c("--no-multiarch"))
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

        connectionDetails <- DatabaseConnector::createConnectionDetails(dbms = dbms,
                                                                        connectionString = connectionString,
                                                                        user = user,
                                                                        password = pwd,
                                                                        pathToDriver = driversPath)

        outputFolder <- file.path(getwd(), 'results')
        dir.create(outputFolder)

        execute(connectionDetails = connectionDetails,
                cdmDatabaseSchema = cdmDatabaseSchema,
                cohortDatabaseSchema = cohortsDatabaseSchema,
                cohortTable = cohortTable,
                outputFolder = outputFolder,
                createCohorts = T,
                runAnalyses = T,
                createValidationPackage = F,
                packageResults = T,
                minCellCount = 5,
                cdmVersion = 5)

        populateShinyApp( outputDirectory = file.path(getwd(), 'ShinyApp'), resultDirectory = outputFolder)
}, finally = {
        remove.packages('{{packageName}}')
})
