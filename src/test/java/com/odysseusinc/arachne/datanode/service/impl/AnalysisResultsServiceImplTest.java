package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFileType;
import com.odysseusinc.arachne.datanode.repository.AnalysisFileRepository;
import com.odysseusinc.arachne.datanode.repository.AnalysisRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnalysisResultsServiceImplTest {

    @Mock
    private AnalysisFileRepository analysisFileRepository;
    @Mock
    private AnalysisRepository analysisRepository;
    @Mock
    private Analysis analysis;
    @InjectMocks
    private AnalysisResultsServiceImpl analysisResultsService;
    private final long analysisId = -1000;

    @Before
    public void setUp() throws Exception {

        when(analysis.getId()).thenReturn(analysisId);
    }

    @Test
    public void shouldReturnAnalysisResultsFiles() {

        analysisResultsService.getAnalysisResults(analysis);

        verify(analysisFileRepository).findAllByAnalysisIdAndType(analysisId, AnalysisFileType.ANALYSYS_RESULT);
    }
}