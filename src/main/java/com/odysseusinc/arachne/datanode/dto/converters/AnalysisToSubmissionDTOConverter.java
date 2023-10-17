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
 * Created: Jul 10, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.environment.EnvironmentDescriptor;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.dto.submission.SubmissionDTO;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisState;
import java.util.Optional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

/**
 * @author vkoulakov
 * @since 4/12/17.
 */
@Component
public class AnalysisToSubmissionDTOConverter implements Converter<Analysis, SubmissionDTO>, InitializingBean {

    @Autowired
    private GenericConversionService conversionService;

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public SubmissionDTO convert(Analysis analysis) {

        SubmissionDTO dto = new SubmissionDTO();
        dto.setAnalysis(analysis.getTitle());
        dto.setId(analysis.getId());
        dto.setOrigin(analysis.getOrigin());
        dto.setStudy(analysis.getStudyTitle());
        DataSource dataSource = analysis.getDataSource();
        if (dataSource != null && conversionService.canConvert(dataSource.getClass(), DataSourceDTO.class)) {
            dto.setDataSource(conversionService.convert(dataSource, DataSourceDTO.class));
        }
        dto.setAuthor(analysis.getAuthor());
        AnalysisState state = analysis.getState();
        if (state != null) {
            dto.setStatus(state.toString());
        }
        dto.setSubmitted(analysis.getSubmitted());
        dto.setFinished(analysis.getFinished());
        EnvironmentDescriptor environment = Optional.ofNullable(analysis.getActualEnvironment()).orElseGet(analysis::getEnvironment);
        dto.setEnvironment(Optional.ofNullable(environment).map(EnvironmentDescriptor::getLabel).orElse(null));
        return dto;
    }
}
