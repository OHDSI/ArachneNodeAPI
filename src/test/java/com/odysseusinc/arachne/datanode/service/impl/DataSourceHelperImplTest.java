package com.odysseusinc.arachne.datanode.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.service.CohortService;
import com.odysseusinc.arachne.datanode.service.DataSourceHelper;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.support.GenericConversionService;

@ExtendWith(MockitoExtension.class)
public class DataSourceHelperImplTest {

    public static final String CALLBACK_PASSWORD = "callbackPassword";
    public static final String UPDATE_STATUS_CALLBACK = "updateStatusCallback";
    public static final String RESULT_CALLBACK = "resultCallback";
    public static final String DATABASE_URL = "database-url";
    public static final String DATANODE_PORT = "8080";
    @Mock
    private GenericConversionService conversionService;
    @Mock
    private CohortService cohortService;
    @Mock
    private DataSourceHelper dataSourceHelper;
    @Mock
    private DataSource dataSource;


    @BeforeEach
    public void setUp() {
        dataSourceHelper = spy(new DataSourceHelperImpl(conversionService, cohortService, DATABASE_URL, DATANODE_PORT));
    }

    @Test
    public void getAnalysisRequestDTO() throws Exception {
        AnalysisRequestDTO requestDTO = new AnalysisRequestDTO();
        requestDTO.setCallbackPassword(CALLBACK_PASSWORD);
        requestDTO.setUpdateStatusCallback(UPDATE_STATUS_CALLBACK);
        requestDTO.setResultCallback(RESULT_CALLBACK);

        when(conversionService.convert(any(), any(Class.class)))
                .thenReturn(requestDTO);

        when(dataSource.getType()).thenReturn(DBMSType.POSTGRESQL);

        String path = "/somepath";
        Path tempDirectory = Files.createTempDirectory("data-source-helper-test");

        AnalysisRequestDTO analysisRequestDTO = dataSourceHelper.prepareRequest(dataSource, tempDirectory, 123L, path);
        assertNotNull(analysisRequestDTO);
        assertEquals(CALLBACK_PASSWORD, analysisRequestDTO.getCallbackPassword());
        assertEquals(UPDATE_STATUS_CALLBACK, analysisRequestDTO.getUpdateStatusCallback());
        assertEquals(String.format("%s:%s%s", DATABASE_URL, DATANODE_PORT, path), analysisRequestDTO.getResultCallback());
        assertEquals(1, tempDirectory.toFile().list().length);
    }

    @Test
    public void getAnalysisRequestDTO_dataSourceIsNull() throws Exception {

        Path tempDirectory = Files.createTempDirectory("data-source-helper-test");
        assertThrows(IllegalStateException.class,
                () -> dataSourceHelper.prepareRequest(null, tempDirectory, 123L, "/somepath"));
    }

    @Test
    public void getAnalysisRequestDTO_tempDirectoryIsNull() throws Exception {

        Path tempDirectory = Files.createTempDirectory("data-source-helper-test");
        assertThrows(IllegalStateException.class,
                () -> dataSourceHelper.prepareRequest(dataSource, null, 123L, "/somepath"));
    }

    @Test
    public void getAnalysisRequestDTO_PathIsEmpty() throws Exception {

        Path tempDirectory = Files.createTempDirectory("data-source-helper-test");
        assertThrows(IllegalStateException.class,
                () -> dataSourceHelper.prepareRequest(dataSource, tempDirectory, 123L, ""));
    }

}