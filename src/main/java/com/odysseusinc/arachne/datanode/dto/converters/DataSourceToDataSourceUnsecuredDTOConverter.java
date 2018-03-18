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

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class DataSourceToDataSourceUnsecuredDTOConverter implements Converter<DataSource, DataSourceUnsecuredDTO>, InitializingBean {

    private GenericConversionService conversionService;
    @Value("${cohorts.result.defaultTargetTable}")
    private String defaultCohortTargetTable;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DataSourceToDataSourceUnsecuredDTOConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;

    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);

    }

    @Override
    public DataSourceUnsecuredDTO convert(DataSource source) {

        DataSourceUnsecuredDTO target = new DataSourceUnsecuredDTO();
        target.setConnectionString(source.getConnectionString());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        final String cdmSchema = source.getCdmSchema();
        target.setCdmSchema(cdmSchema);
        target.setType(DBMSType.valueOf(source.getType().name()));
        final String targetSchema = source.getTargetSchema();
        target.setTargetSchema(StringUtils.isEmpty(targetSchema) ? cdmSchema : targetSchema);
        final String resultSchema = source.getResultSchema();
        target.setResultSchema(StringUtils.isEmpty(resultSchema) ? cdmSchema : resultSchema);
        final String cohortTargetTable = source.getCohortTargetTable();
        target.setCohortTargetTable(StringUtils.isEmpty(cohortTargetTable) ? defaultCohortTargetTable : cohortTargetTable);

        return target;
    }
}
