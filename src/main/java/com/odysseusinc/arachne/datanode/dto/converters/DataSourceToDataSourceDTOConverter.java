/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: April 20, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceDTO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class DataSourceToDataSourceDTOConverter implements Converter<DataSource, DataSourceDTO>, InitializingBean {

    private GenericConversionService conversionService;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DataSourceToDataSourceDTOConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;

    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);

    }

    @Override
    public DataSourceDTO convert(DataSource source) {

        DataSourceDTO target = new DataSourceDTO();
        target.setConnectionString(source.getConnectionString());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setCdmSchema(source.getCdmSchema());
        target.setType(source.getType() != null ? DBMSType.valueOf(source.getType().name()) : null);
        target.setTargetSchema(source.getTargetSchema());
        target.setResultSchema(source.getResultSchema());
        target.setCohortTargetTable(source.getCohortTargetTable());
        return target;
    }
}
