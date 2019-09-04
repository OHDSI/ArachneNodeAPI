package com.odysseusinc.arachne.datanode.model.analysis;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "analysis_state_journal")
@Embeddable
public class AnalysisStateEntry {

    @Id
    @SequenceGenerator(name = "analyses_state_pk_sequence", sequenceName = "analysis_state_journal_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analyses_state_pk_sequence")
    private Long id;
    @Column(name = "date")
    private Date date;
    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private AnalysisState state;
    @Column(name = "reason")
    private String reason;
    @ManyToOne(fetch = FetchType.EAGER)
    private Analysis analysis;

    public AnalysisStateEntry() {

    }

    public AnalysisStateEntry(Date date, AnalysisState state, String reason, Analysis analysis) {

        this.date = date;
        this.state = state;
        this.reason = reason;
        this.analysis = analysis;

    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public Date getDate() {

        return date;
    }

    public void setDate(Date date) {

        this.date = date;
    }

    public AnalysisState getState() {

        return state;
    }

    public void setState(AnalysisState state) {

        this.state = state;
    }

    public String getReason() {

        return reason;
    }

    public void setReason(String reason) {

        this.reason = reason;
    }

    public Analysis getAnalysis() {

        return analysis;
    }

    public void setAnalysis(Analysis analysis) {

        this.analysis = analysis;
    }
}
