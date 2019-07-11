/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Jul 8, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;


import com.odysseusinc.arachne.commons.utils.UUIDGenerator;
import com.odysseusinc.arachne.datanode.controller.analysis.CallbackAnalysisController;
import com.odysseusinc.arachne.datanode.dto.analysis.AnalysisRequestDTO;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisState;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisStateEntry;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import com.odysseusinc.arachne.datanode.util.AnalysisUtils;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class AnalysisRequestDTOToAnalysisConverter implements Converter<AnalysisRequestDTO, Analysis> {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisRequestDTOToAnalysisConverter.class);

    @Value("${datanode.baseURL}")
    private String datanodeBaseURL;
    @Value("${datanode.port}")
    private String datanodePort;
    @Value("${files.store.path}")
    private String filesStorePath;

    private final DataSourceService dataSourceService;

    public AnalysisRequestDTOToAnalysisConverter(GenericConversionService conversionService,
                                                 DataSourceService dataSourceService) {

        this.dataSourceService = dataSourceService;
        conversionService.addConverter(this);
    }

    @Override
    public Analysis convert(AnalysisRequestDTO dto) {

        Analysis analysis = new Analysis();

        analysis.setExecutableFileName(dto.getExecutableFileName());
        final String analysisFolder = AnalysisUtils.createUniqueDir(filesStorePath).getAbsolutePath();
        analysis.setAnalysisFolder(analysisFolder);

        analysis.setTitle(dto.getTitle());
        if (StringUtils.isNotBlank(dto.getStudy())) {
            analysis.setStudyTitle(dto.getStudy());
        }

        DataSource dataSource = dataSourceService.getById(dto.getDatasourceId());
        if (Objects.isNull(dataSource)) {
            logger.error("Cannot find datasource with id: {}", dto.getDatasourceId());
            throw new NotExistException(DataSource.class);
        }
        analysis.setDataSource(dataSource);

        AnalysisStateEntry stateEntry = new AnalysisStateEntry(new Date(),
                AnalysisState.CREATED,
                "Get request to execute analysis",
                analysis);
        analysis.getStateHistory().add(stateEntry);

        analysis.setCallbackPassword(UUIDGenerator.generateUUID());
        String updateStatusCallback = String.format(
                "%s:%s%s",
                datanodeBaseURL,
                datanodePort,
                CallbackAnalysisController.UPDATE_URI
        );
        String resultCallback = String.format(
                "%s:%s%s",
                datanodeBaseURL,
                datanodePort,
                CallbackAnalysisController.RESULT_URI
        );
        analysis.setUpdateStatusCallback(updateStatusCallback);
        analysis.setResultCallback(resultCallback);

        return analysis;
    }
}
