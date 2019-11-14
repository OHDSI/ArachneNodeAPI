package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.repository.DataSourceRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonModelType;
import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.repository.DataSourceRepository;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.DataSourceHelper;
import com.odysseusinc.arachne.datanode.service.ExecutionEngineIntegrationService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@RunWith(MockitoJUnitRunner.class)
public class DataSourceServiceImplTest {

    private DataSourceServiceImpl dataSourceService;

    @Mock
    private DataSourceRepository dataSourceRepository;
    @Mock
    private DataNodeService dataNodeService;
    @Mock
    private GenericConversionService conversionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BaseCentralIntegrationService<DataSource, CommonDataSourceDTO> integrationService;
    @Mock
    private CentralClient centralClient;
    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private DataSourceHelper dataSourceHelper;
    @Mock
    private ExecutionEngineIntegrationService engineIntegrationService;
    @Mock
    private DestinationResolver destinationResolver;

    @Captor
    private ArgumentCaptor<CommonDataSourceDTO> dataSourceCaptor;
    private AnalysisResultDTO result;
    public InputStream inputStream = IOUtils.toInputStream("cdm_version: V5_0", StandardCharsets.UTF_8);


    @Before
    public void setUp() {

        when(jmsTemplate.getDestinationResolver()).thenReturn(destinationResolver);
        dataSourceService = spy(new DataSourceServiceImpl(
                dataSourceRepository, dataNodeService, integrationService, conversionService, eventPublisher, centralClient, jmsTemplate, dataSourceHelper, engineIntegrationService, 4000L
        ));
        result = new AnalysisResultDTO();
        result.setStatus(AnalysisResultStatusDTO.EXECUTED);

    }

    @Test
    public void firstCheckCallbackProcess() throws Exception {

        MultipartFile file = new MockMultipartFile("cdm_version.txt","cdm_version.txt", "text", inputStream);
        dataSourceService.firstCheckCallbackProcess(1L, "password", result, ArrayUtils.toArray(file));

        verify(dataSourceService).putResponseToQueue(any(), any(), dataSourceCaptor.capture());
        assertEquals(CommonModelType.CDM, dataSourceCaptor.getValue().getModelType());
        assertEquals(CommonCDMVersionDTO.V5_0, dataSourceCaptor.getValue().getCdmVersion());
    }

    @Test
    public void firstCheckCallbackProcess_noFiles() throws Exception {

        dataSourceService.firstCheckCallbackProcess(1L, "password", result, null);

        verify(dataSourceService).putResponseToQueue(any(), any(), dataSourceCaptor.capture());
        assertEquals(CommonModelType.OTHER, dataSourceCaptor.getValue().getModelType());
        assertNull(dataSourceCaptor.getValue().getCdmVersion());
    }

    @Test
    public void firstCheckCallbackProcess_noCdmVersionFiles() throws Exception {

        inputStream = IOUtils.toInputStream("somevalues", StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile("somefile.txt","somefile.txt", "text", inputStream);
        dataSourceService.firstCheckCallbackProcess(1L, "password", result, ArrayUtils.toArray(file));

        verify(dataSourceService).putResponseToQueue(any(), any(), dataSourceCaptor.capture());
        assertEquals(CommonModelType.OTHER, dataSourceCaptor.getValue().getModelType());
        assertNull(dataSourceCaptor.getValue().getCdmVersion());
    }

    @Test
    public void firstCheckCallbackProcess_noVersionInCdmFiles() throws Exception {

        inputStream = IOUtils.toInputStream("somevalues", StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile("cdm_version.txt","cdm_version.txt", "text", inputStream);
        dataSourceService.firstCheckCallbackProcess(1L, "password", result, ArrayUtils.toArray(file));

        verify(dataSourceService).putResponseToQueue(any(), any(), dataSourceCaptor.capture());
        assertEquals(CommonModelType.OTHER, dataSourceCaptor.getValue().getModelType());
        assertNull(dataSourceCaptor.getValue().getCdmVersion());
    }

    @Test
    public void firstCheckCallbackProcess_nullResultArgument() throws Exception {
        MultipartFile file = new MockMultipartFile("cdm_version.txt","cdm_version.txt", "text", inputStream);
        dataSourceService.firstCheckCallbackProcess(1L, "password", null, ArrayUtils.toArray(file));

        verify(dataSourceService).putResponseToQueue(any(), any(), dataSourceCaptor.capture());
        assertNull(dataSourceCaptor.getValue().getModelType());
        assertNull(dataSourceCaptor.getValue().getCdmVersion());
    }

    @Test
    public void firstCheckCallbackProcess_FailedResultArgument() throws Exception {
        result.setStatus(AnalysisResultStatusDTO.FAILED);
        MultipartFile file = new MockMultipartFile("cdm_version.txt","cdm_version.txt", "text", inputStream);
        dataSourceService.firstCheckCallbackProcess(1L, "password", null, ArrayUtils.toArray(file));

        verify(dataSourceService).putResponseToQueue(any(), any(), dataSourceCaptor.capture());
        assertNull(dataSourceCaptor.getValue().getModelType());
        assertNull(dataSourceCaptor.getValue().getCdmVersion());
    }

    @Mock
    private DataSource dataSource;
    @Mock
    private DataSourceRepository dataSourceRepository;
    @InjectMocks
    private DataSourceServiceImpl dataSourceService;

    private final Long dataSourceId = 1000L;
    private final String dataSourceName = "dataSourceName";


    @Test
    public void shouldReturnTrueIfDataSourceWithTheSameNameDoesNotExists() {

        when(dataSourceRepository.countByName(any())).thenReturn(0);

        final boolean isUnique = dataSourceService.isDatasourceNameUnique("dataSourceName", dataSourceId);

        assertThat(isUnique).isTrue();
    }

    @Test
    public void shouldReturnTrueIfDataSourceWithTheSameNameAndIdExists() {

        when(dataSource.getId()).thenReturn(dataSourceId);
        when(dataSourceRepository.countByName(dataSourceName)).thenReturn(1);

        final boolean isUnique = dataSourceService.isDatasourceNameUnique(dataSourceName, dataSourceId);

        assertThat(isUnique).isTrue();
    }

    @Test
    public void shouldReturnFalseIfDataSourceWithTheSameNameButAnotherIdExists() {

        when(dataSource.getId()).thenReturn(dataSourceId);
        when(dataSourceRepository.countByIdNotAndName(33L, dataSourceName)).thenReturn(1);

        final boolean isUnique = dataSourceService.isDatasourceNameUnique(dataSourceName, 33L);

        assertThat(isUnique).isFalse();
    }
}