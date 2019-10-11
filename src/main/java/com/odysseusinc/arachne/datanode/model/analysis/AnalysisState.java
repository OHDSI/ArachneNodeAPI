package com.odysseusinc.arachne.datanode.model.analysis;

public enum AnalysisState {
    CREATED("CREATED"),
    FILES_DOWNLOAD_FAILURE("FILES DOWNLOAD FAILURE"),
    EXECUTION_READY("EXECUTION READY"),
    EXECUTION_FAILURE("EXECUTION FAILURE"),
    EXECUTING("EXECUTING"),
    EXECUTED("EXECUTED"),
    SENDING_TO_CENTRAL_FAILURE("SENDING TO CENTRAL FAILURE"),
    CLOSED("CLOSED"),
    DEAD("DEAD");

    private String title;

    AnalysisState(String title) {

        this.title = title;
    }
}
