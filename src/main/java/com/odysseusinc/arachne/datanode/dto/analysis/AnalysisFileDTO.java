package com.odysseusinc.arachne.datanode.dto.analysis;

import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFileStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalysisFileDTO {
    private String path;
    private String contentType;
    private AnalysisFileStatus status;
}
