package com.odysseusinc.arachne.datanode.dto.submission;

import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisAuthor;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisOrigin;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionDTO {
    private Long id;
    private String study;
    private String analysis;
    private AnalysisOrigin origin;
    private DataSourceDTO dataSource;
    private String status;
    private AnalysisAuthor author;
    private Date submitted;
    private Date finished;
    private String environment;
}
