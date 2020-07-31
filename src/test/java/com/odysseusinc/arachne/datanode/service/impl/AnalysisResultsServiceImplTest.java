package com.odysseusinc.arachne.datanode.service.impl;

import static java.nio.file.Files.copy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.Files;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFile;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFileType;
import com.odysseusinc.arachne.datanode.repository.AnalysisFileRepository;
import com.odysseusinc.arachne.datanode.repository.AnalysisRepository;
import com.odysseusinc.arachne.datanode.service.Const;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void setUp() {

        when(analysis.getId()).thenReturn(analysisId);
        when(analysis.getStatus()).thenReturn(AnalysisResultStatusDTO.EXECUTED);
        testWorkingDir = Files.createTempDir();
        testWorkingDir.deleteOnExit();
    }

    @Test
    public void shouldReturnAnalysisResultsFiles() {

        analysisResultsService.getAnalysisResults(analysis);

        verify(analysisFileRepository).findAllByAnalysisIdAndType(analysisId, AnalysisFileType.ANALYSYS_RESULT);
    }

    @Test
    public void shouldCreateAndSaveTwoAnalysisResultFileEntities() throws IOException {

        final File tempFileOne = File.createTempFile("test", "resultFile1", testWorkingDir);
        final File tempFileTwo = File.createTempFile("test", "resultFile2", testWorkingDir);

        analysisResultsService.saveResults(analysis, testWorkingDir);

        verify(analysisFileRepository).save(captor.capture());
        final List<AnalysisFile> analysisFiles = captor.getValue();
        assertThat(analysisFiles.size()).isEqualTo(2);
        final AnalysisFile file = analysisFiles.get(1);
        assertThat(file.getAnalysis()).isEqualTo(analysis);
        assertThat(analysisFiles).extracting(AnalysisFile::getLink).containsOnly(tempFileTwo.getAbsolutePath(), tempFileOne.getAbsolutePath());
    }

    @Test
    public void shouldSetAnalysisStatusToFailedIfErrorReportFileFound() throws IOException {

        final String zipFile = Const.class.getResource(RESULTS_WITH_ERROR_ZIP).getPath();
        copy(Paths.get(zipFile), testWorkingDir.toPath().resolve(RESULTS_WITH_ERROR_ZIP));

        final Analysis existingAnalysis = new Analysis();
        existingAnalysis.setAnalysisFolder(Files.createTempDir().getAbsolutePath());


        when(analysisRepository.findOne(analysisId)).thenReturn(existingAnalysis);
        when(analysisRepository.save(existingAnalysis)).thenReturn(existingAnalysis);
        final Analysis updatedAnalysis = analysisResultsService.saveResults(analysis, testWorkingDir);

        assertThat(updatedAnalysis.getStatus()).isEqualTo(AnalysisResultStatusDTO.FAILED);
    }

    @Test
    public void shouldLeaveAnalysisStatusIfNoErrorReport() throws IOException {

        final String zipFile = Const.class.getResource(RESULTS_SUCCESSFUL_ZIP).getPath();
        copy(Paths.get(zipFile), testWorkingDir.toPath().resolve(RESULTS_SUCCESSFUL_ZIP));

        final Analysis existingAnalysis = new Analysis();
        existingAnalysis.setAnalysisFolder(Files.createTempDir().getAbsolutePath());


        when(analysisRepository.findOne(analysisId)).thenReturn(existingAnalysis);
        when(analysisRepository.save(existingAnalysis)).thenReturn(existingAnalysis);
        final Analysis updatedAnalysis = analysisResultsService.saveResults(analysis, testWorkingDir);

        assertThat(updatedAnalysis.getStatus()).isEqualTo(AnalysisResultStatusDTO.EXECUTED);
    }

}
