package com.odysseusinc.arachne.datanode.model.analysis;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "analysis_files")
public class AnalysisFile {

    @Id
    @SequenceGenerator(name = "analysis_files_pk_sequence", sequenceName = "analysis_files_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analysis_files_pk_sequence")
    private Long id;
    @Column(name = "type")
    @Enumerated(value = EnumType.STRING)
    private AnalysisFileType type;
    @Column(name = "link")
    private String link;
    @Column(name = "status")
    @Enumerated(value = EnumType.STRING)
    private AnalysisFileStatus status = AnalysisFileStatus.UNPROCESSED;
    @Column(name = "retries")
    private Long retries = 0L;
    @ManyToOne
    private Analysis analysis;

    public AnalysisFile() {

    }

    public AnalysisFile(String link, AnalysisFileType type, Analysis analysis) {

        this.link = link;
        this.type = type;
        this.analysis = analysis;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public AnalysisFileType getType() {

        return type;
    }

    public void setType(AnalysisFileType type) {

        this.type = type;
    }

    public String getLink() {

        return link;
    }

    public void setLink(String link) {

        this.link = link;
    }

    public AnalysisFileStatus getStatus() {

        return status;
    }

    public void setStatus(AnalysisFileStatus status) {

        this.status = status;
    }

    public Long getRetries() {

        return retries;
    }

    public void setRetries(Long retries) {

        this.retries = retries;
    }

    public Analysis getAnalysis() {

        return analysis;
    }

    public void setAnalysis(Analysis analysis) {

        this.analysis = analysis;
    }
}
