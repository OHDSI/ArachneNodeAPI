packratRun <- function (packratPath, workDir, func){
  tryCatch(
    {
      tryCatch(
        {
          if (!require("packrat")) {
            install.packages("packrat")
          }
          setwd(workDir)
          packrat::unbundle(bundle = packratPath, where = workDir)
          setwd("./PatientLevelPredictionAnalysis")
        },
        error = function(e) {
          setwd("./PatientLevelPredictionAnalysis")
        },
        finally = {
          packrat_mode(on=TRUE)
          writeLines("Session library paths:")
          print(.libPaths())
          do.call(func, list(workDir))
        }
      )
    },
    finally = {
      setwd(workDir)
      writeLines("Session info:")
      print(sessionInfo())
      if (file.exists("./PatientLevelPredictionAnalysis")) {
        unlink("./PatientLevelPredictionAnalysis", recursive = TRUE)
      }
    }
  )
}
