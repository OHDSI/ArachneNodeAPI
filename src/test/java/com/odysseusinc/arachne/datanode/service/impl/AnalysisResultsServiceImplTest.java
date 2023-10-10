package com.odysseusinc.arachne.datanode.service.impl;

import com.google.common.io.Files;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFile;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFileType;
import com.odysseusinc.arachne.datanode.repository.AnalysisFileRepository;
import com.odysseusinc.arachne.datanode.repository.AnalysisRepository;
import com.odysseusinc.arachne.datanode.service.Const;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static java.nio.file.Files.copy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalysisResultsServiceImplTest {

    private final long analysisId = -1000;
    public static final String RESULTS_WITH_ERROR_ZIP = "results-with-error.zip";
    public static final String RESULTS_SUCCESSFUL_ZIP = "results-successful.zip";
    @Mock
    private AnalysisFileRepository analysisFileRepository;
    @Mock
    private AnalysisRepository analysisRepository;
    @Mock
    private Analysis analysis;
    @InjectMocks
    private AnalysisResultsServiceImpl analysisResultsService;
    private File testWorkingDir;

    @Captor
    private ArgumentCaptor<List<AnalysisFile>> captor;

    @BeforeEach
    public void setUp() {
        testWorkingDir = Files.createTempDir();
        testWorkingDir.deleteOnExit();
    }

    @Test
    public void shouldReturnAnalysisResultsFiles() {
        when(analysis.getId()).thenReturn(analysisId);
        analysisResultsService.getAnalysisResults(analysis);

        verify(analysisFileRepository).findAllByAnalysisIdAndType(analysisId, AnalysisFileType.ANALYSYS_RESULT);
    }

    @Test
    public void shouldCreateAndSaveTwoAnalysisResultFileEntities() throws IOException {
        when(analysis.getId()).thenReturn(analysisId);

        final File tempFileOne = File.createTempFile("test", "resultFile1", testWorkingDir);
        final File tempFileTwo = File.createTempFile("test", "resultFile2", testWorkingDir);

        analysisResultsService.saveResults(analysis, testWorkingDir);

        verify(analysisFileRepository).saveAll(captor.capture());
        final List<AnalysisFile> analysisFiles = captor.getValue();
        assertThat(analysisFiles.size()).isEqualTo(2);
        final AnalysisFile file = analysisFiles.get(1);
        assertThat(file.getAnalysis()).isEqualTo(analysis);
        assertThat(analysisFiles).extracting(AnalysisFile::getLink).containsOnly(tempFileTwo.getAbsolutePath(), tempFileOne.getAbsolutePath());
    }

    @Test
    public void shouldSetAnalysisStatusToFailedIfErrorReportFileFound() throws IOException {
        when(analysis.getId()).thenReturn(analysisId);
        when(analysis.getStatus()).thenReturn(AnalysisResultStatusDTO.EXECUTED);

        final String zipFile = new File(Const.class.getResource(RESULTS_WITH_ERROR_ZIP).getFile()).getPath();
        copy(Paths.get(zipFile), testWorkingDir.toPath().resolve(RESULTS_WITH_ERROR_ZIP));

        final Analysis existingAnalysis = new Analysis();
        existingAnalysis.setAnalysisFolder(Files.createTempDir().getAbsolutePath());


        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(existingAnalysis));
        when(analysisRepository.save(existingAnalysis)).thenReturn(existingAnalysis);
        final Analysis updatedAnalysis = analysisResultsService.saveResults(analysis, testWorkingDir);

        assertThat(updatedAnalysis.getStatus()).isEqualTo(AnalysisResultStatusDTO.FAILED);
    }

    @Test
    public void shouldLeaveAnalysisStatusIfNoErrorReport() throws IOException {
        when(analysis.getId()).thenReturn(analysisId);
        when(analysis.getStatus()).thenReturn(AnalysisResultStatusDTO.EXECUTED);

        final String zipFile = new File(Const.class.getResource(RESULTS_SUCCESSFUL_ZIP).getFile()).getPath();
        copy(Paths.get(zipFile), testWorkingDir.toPath().resolve(RESULTS_SUCCESSFUL_ZIP));

        final Analysis existingAnalysis = new Analysis();
        existingAnalysis.setAnalysisFolder(Files.createTempDir().getAbsolutePath());


        when(analysisRepository.findById(analysisId)).thenReturn(Optional.of(existingAnalysis));
        when(analysisRepository.save(existingAnalysis)).thenReturn(existingAnalysis);
        final Analysis updatedAnalysis = analysisResultsService.saveResults(analysis, testWorkingDir);

        assertThat(updatedAnalysis.getStatus()).isEqualTo(AnalysisResultStatusDTO.EXECUTED);
    }

}
