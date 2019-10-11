package com.odysseusinc.arachne.datanode.model.analysis;

public enum AnalysisFileStatus {
    UNPROCESSED("UNPROCESSED"),
    IGNORED("IGNORED"),
    PROCESSED("PROCESSED"),
    FAILED("FAILED");

    private String title;

    AnalysisFileStatus(String title) {

        this.title = title;
    }

    public String getTitle() {

        return title;
    }
}
