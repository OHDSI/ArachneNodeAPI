package com.odysseusinc.arachne.datanode.event;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import java.io.File;
import org.springframework.context.ApplicationEvent;

public class AnalysisResultEvent extends ApplicationEvent {

    private AnalysisResultDTO analysisResult;
    private File analysisResultFolder;

    public AnalysisResultEvent(Object source, AnalysisResultDTO analysisResult, File analysisResultFolder) {

        super(source);
        this.analysisResult = analysisResult;
        this.analysisResultFolder = analysisResultFolder;
    }

    public AnalysisResultDTO getAnalysisResult() {

        return analysisResult;
    }

    public void setAnalysisResult(AnalysisResultDTO analysisResult) {

        this.analysisResult = analysisResult;
    }

    public File getAnalysisResultFolder() {

        return analysisResultFolder;
    }

    public void setAnalysisResultFolder(File analysisResultFolder) {

        this.analysisResultFolder = analysisResultFolder;
    }
}
