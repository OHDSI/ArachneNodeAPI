package com.odysseusinc.arachne.datanode.dto.submission;

import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisAuthor;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisOrigin;
import java.util.Date;

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

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getStudy() {

        return study;
    }

    public void setStudy(String study) {

        this.study = study;
    }

    public String getAnalysis() {

        return analysis;
    }

    public void setAnalysis(String analysis) {

        this.analysis = analysis;
    }

    public DataSourceDTO getDataSource() {

        return dataSource;
    }

    public void setDataSource(DataSourceDTO dataSource) {

        this.dataSource = dataSource;
    }

    public String getStatus() {

        return status;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public AnalysisAuthor getAuthor() {

        return author;
    }

    public void setAuthor(AnalysisAuthor author) {

        this.author = author;
    }

    public Date getSubmitted() {

        return submitted;
    }

    public void setSubmitted(Date submitted) {

        this.submitted = submitted;
    }

    public Date getFinished() {

        return finished;
    }

    public void setFinished(Date finished) {

        this.finished = finished;
    }

    public AnalysisOrigin getOrigin() {

        return origin;
    }

    public void setOrigin(AnalysisOrigin origin) {

        this.origin = origin;
    }
}
