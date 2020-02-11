package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class DataSourceToAnalysisRequestDTOConverter
        implements Converter<DataSource, AnalysisRequestDTO>, InitializingBean {

    private GenericConversionService conversionService;

    @Value("${datanode.baseURL}")
    private String datanodeBaseURL;
    @Value("${datanode.port}")
    private String datanodePort;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DataSourceToAnalysisRequestDTOConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public AnalysisRequestDTO convert(DataSource dataSource) {

        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        DataSourceUnsecuredDTO dataSourceDTO = conversionService.convert(dataSource, DataSourceUnsecuredDTO.class);
        analysisRequestDTO.setDataSource(dataSourceDTO);
        analysisRequestDTO.setExecutableFileName("check_datasource.sql");

        String base = String.format("%s:%s", datanodeBaseURL, datanodePort);
        analysisRequestDTO.setUpdateStatusCallback(base + Constants.Api.DataSource.DS_MODEL_CHECK_UPDATE);
        analysisRequestDTO.setCallbackPassword(UUID.randomUUID().toString());
        analysisRequestDTO.setRequested(new Date());
        return analysisRequestDTO;
    }
}
