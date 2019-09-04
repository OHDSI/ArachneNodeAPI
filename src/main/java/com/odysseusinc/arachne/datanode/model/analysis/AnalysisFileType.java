package com.odysseusinc.arachne.datanode.model.analysis;

public enum AnalysisFileType {
    ANALYSIS("ANALYSIS"),
    ANALYSYS_RESULT("ANALYSIS_RESULT");

    private String title;

    AnalysisFileType(String title) {

        this.title = title;
    }

    public String getTitle() {

        return title;
    }
}
