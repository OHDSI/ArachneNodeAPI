package com.odysseusinc.arachne.datanode.model.analysis;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.datanode.environment.EnvironmentDescriptor;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "analyses")
public class Analysis {

    @SequenceGenerator(name = "analyses_id_seq", sequenceName = "analyses_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analyses_id_seq")
    @Id
    private Long id;
    @Column(name = "central_id")
    private Long centralId;
    @NotNull
    @Column(name = "executable_filename")
    private String executableFileName;
    @NotNull
    @Column(name = "callback_password")
    private String callbackPassword;
    @NotNull
    @Column(name = "update_status_callback")
    private String updateStatusCallback;
    @NotNull
    @Column(name = "result_callback")
    private String resultCallback;
    @NotNull
    @ManyToOne
    private DataSource dataSource;
    @NotNull
    @Column(name = "analysis_folder")
    private String analysisFolder;
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "analysis")
    private List<AnalysisStateEntry> stateHistory = new ArrayList<>();
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "analysis")
    private List<AnalysisFile> analysisFiles = new ArrayList<>();
    @Column(name = "stdout")
    private String stdout;
    @Column(name = "result_status")
    @Enumerated(value = EnumType.STRING)
    private AnalysisResultStatusDTO status;
    @Column(name = "title")
    private String title;
    @Column(name = "study_title")
    private String studyTitle;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "email", column = @Column(name = "author_email")),
            @AttributeOverride(name = "firstName", column = @Column(name = "author_first_name")),
            @AttributeOverride(name = "lastName", column = @Column(name = "author_last_name"))
    })
    private AnalysisAuthor author;
    @Transient
    private AnalysisState state;
    @Transient
    private Date submitted;
    @Transient
    private Date finished;
    @Column
    @Enumerated(EnumType.STRING)
    private CommonAnalysisType type;
    @Column
    @Enumerated(EnumType.STRING)
    private AnalysisOrigin origin;
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "analysis")
    private List<AnalysisCodeFile> analysisCodeFiles = new ArrayList<>();
    @Column(name = "inner_executable_filename")
    private String innerExecutableFilename;
    @ManyToOne
    @JoinColumn(name = "environment_id")
    private EnvironmentDescriptor environment;
    @ManyToOne
    @JoinColumn(name = "actual_environment_id")
    private EnvironmentDescriptor actualEnvironment;


    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public Long getCentralId() {

        return centralId;
    }

    public void setCentralId(Long centralId) {

        this.centralId = centralId;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle(String title) {

        this.title = title;
    }

    public String getExecutableFileName() {

        return executableFileName;
    }

    public void setExecutableFileName(String executableFileName) {

        this.executableFileName = executableFileName;
    }

    public String getCallbackPassword() {

        return callbackPassword;
    }

    public void setCallbackPassword(String callbackPassword) {

        this.callbackPassword = callbackPassword;
    }

    public String getUpdateStatusCallback() {

        return updateStatusCallback;
    }

    public void setUpdateStatusCallback(String updateStatusCallback) {

        this.updateStatusCallback = updateStatusCallback;
    }

    public String getResultCallback() {

        return resultCallback;
    }

    public void setResultCallback(String resultCallback) {

        this.resultCallback = resultCallback;
    }

    public DataSource getDataSource() {

        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {

        this.dataSource = dataSource;
    }

    public String getAnalysisFolder() {

        return analysisFolder;
    }

    public void setAnalysisFolder(String analysisFolder) {

        this.analysisFolder = analysisFolder;
    }

    public List<AnalysisFile> getAnalysisFiles() {

        return analysisFiles;
    }

    public void setAnalysisFiles(List<AnalysisFile> analysisFiles) {

        this.analysisFiles = analysisFiles;
    }

    public List<AnalysisStateEntry> getStateHistory() {

        return stateHistory;
    }

    public void setStateHistory(List<AnalysisStateEntry> stateHistory) {

        this.stateHistory = stateHistory;
    }

    public String getStdout() {

        return stdout;
    }

    public void setStdout(String stdout) {

        this.stdout = stdout;
    }

    public AnalysisResultStatusDTO getStatus() {

        return status;
    }

    public void setStatus(AnalysisResultStatusDTO status) {

        this.status = status;
    }

    public AnalysisAuthor getAuthor() {

        return author;
    }

    public void setAuthor(AnalysisAuthor author) {

        this.author = author;
    }

    public AnalysisState getState() {

        return state;
    }

    public Date getSubmitted() {

        return submitted;
    }

    public Date getFinished() {

        return finished;
    }

    @PostLoad
    public void postLoad() {

        Comparator<AnalysisStateEntry> comparator = Comparator.comparing(AnalysisStateEntry::getDate);
        Optional<List<AnalysisStateEntry>> analysisStateHistory = Optional.ofNullable(this.stateHistory);
        analysisStateHistory.ifPresent(stateHistory -> {
            Optional<AnalysisStateEntry> latestState =
                    stateHistory.stream()
                            .filter(h -> h.getDate() != null)
                            .max(Comparator.nullsFirst(comparator));
            latestState.ifPresent(s -> {
                state = s.getState();
                if (s.getState() != AnalysisState.CREATED
                        && s.getState() != AnalysisState.EXECUTING
                        && s.getState() != AnalysisState.EXECUTION_READY) {
                    finished = s.getDate();
                }
            });
            Optional<AnalysisStateEntry> created = stateHistory.stream()
                    .filter(h -> h.getState() == AnalysisState.CREATED).findFirst();
            created.ifPresent(h -> this.submitted = h.getDate());
        });
    }

    public CommonAnalysisType getType() {

        return type;
    }

    public void setType(CommonAnalysisType type) {

        this.type = type;
    }

    public List<AnalysisCodeFile> getAnalysisCodeFiles() {

        return analysisCodeFiles;
    }

    public void setAnalysisCodeFiles(List<AnalysisCodeFile> analysisCodeFiles) {

        this.analysisCodeFiles = analysisCodeFiles;
    }

    public String getInnerExecutableFilename() {

        return innerExecutableFilename;
    }

    public void setInnerExecutableFilename(String exectubleInnerFileName) {

        this.innerExecutableFilename = exectubleInnerFileName;
    }

    public String getStudyTitle() {

        return studyTitle;
    }

    public void setStudyTitle(String studyTitle) {

        this.studyTitle = studyTitle;
    }

    public AnalysisOrigin getOrigin() {

        return origin;
    }

    public void setOrigin(AnalysisOrigin origin) {

        this.origin = origin;
    }
}
