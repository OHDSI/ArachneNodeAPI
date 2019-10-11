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

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import java.util.Date;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class AnalysisToAnalysisRequestDTOConverter
        implements Converter<Analysis, AnalysisRequestDTO>, InitializingBean {

    private GenericConversionService conversionService;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public AnalysisToAnalysisRequestDTOConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;

    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public AnalysisRequestDTO convert(Analysis analysis) {

        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        DataSource dataSource = analysis.getDataSource();
        DataSourceUnsecuredDTO dataSourceDTO = conversionService.convert(dataSource, DataSourceUnsecuredDTO.class);
        analysisRequestDTO.setDataSource(dataSourceDTO);
        analysisRequestDTO.setId(analysis.getId());
        analysisRequestDTO.setExecutableFileName(analysis.getExecutableFileName());
        analysisRequestDTO.setUpdateStatusCallback(analysis.getUpdateStatusCallback());
        analysisRequestDTO.setResultCallback(analysis.getResultCallback());
        analysisRequestDTO.setCallbackPassword(analysis.getCallbackPassword());
        analysisRequestDTO.setRequested(new Date());
        return analysisRequestDTO;
    }
}
