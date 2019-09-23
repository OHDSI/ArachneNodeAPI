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
  workDir <- getwd()
  dbms <- Sys.getenv("DBMS_TYPE")
  connStr <- Sys.getenv("CONNECTION_STRING")
  user <- Sys.getenv("DBMS_USERNAME")
  pw <- Sys.getenv("DBMS_PASSWORD")
  cdmDatabaseSchema <- Sys.getenv("DBMS_SCHEMA")
  resultsDatabaseSchema <- Sys.getenv("RESULT_SCHEMA")
  cohortsDatabaseSchema <- Sys.getenv("TARGET_SCHEMA")
  cohorts <- list({{{cohortDefinitions}}})
  run_ir_analysis(workDir, {{analysisId}}, cohorts, dbms, connStr, user, pw, cdmDatabaseSchema, resultsDatabaseSchema, cohortsDatabaseSchema, cohortTable = "cohort", outcomeTable = "cohort")
}, finally = {
  remove.packages('{{packageName}}', lib = libs_local)
  unlink(libs_local, recursive = TRUE, force = TRUE)
})