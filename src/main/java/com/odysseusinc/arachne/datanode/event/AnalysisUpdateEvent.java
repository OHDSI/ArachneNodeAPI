package com.odysseusinc.arachne.datanode.event;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisExecutionStatusDTO;
import org.springframework.context.ApplicationEvent;

public class AnalysisUpdateEvent extends ApplicationEvent {

    private AnalysisExecutionStatusDTO analysisExecutionStatus;
    private String password;

    public AnalysisUpdateEvent(Object source, AnalysisExecutionStatusDTO analysisExecutionStatus, String password) {

        super(source);
        this.analysisExecutionStatus = analysisExecutionStatus;
        this.password = password;
    }

    public AnalysisExecutionStatusDTO getAnalysisExecutionStatus() {

        return analysisExecutionStatus;
    }

    public void setAnalysisExecutionStatus(AnalysisExecutionStatusDTO analysisExecutionStatus) {

        this.analysisExecutionStatus = analysisExecutionStatus;
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(String password) {

        this.password = password;
    }
}
