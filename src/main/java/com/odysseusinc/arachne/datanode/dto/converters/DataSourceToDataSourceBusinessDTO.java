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
 * Created: December 19, 2016
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceBusinessDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class DataSourceToDataSourceBusinessDTO implements Converter<DataSource, DataSourceBusinessDTO>, InitializingBean {

    private final GenericConversionService conversionService;

    public DataSourceToDataSourceBusinessDTO(GenericConversionService conversionService) {

        this.conversionService = conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public DataSourceBusinessDTO convert(DataSource source) {

        final DataSourceBusinessDTO dataSourceBusinessDTO = new DataSourceBusinessDTO();
        dataSourceBusinessDTO.setId(source.getId());
        dataSourceBusinessDTO.setTargetSchema(source.getTargetSchema());
        dataSourceBusinessDTO.setResultSchema(source.getResultSchema());
        dataSourceBusinessDTO.setCohortTargetTable(source.getCohortTargetTable());
        dataSourceBusinessDTO.setName(source.getName());
        return dataSourceBusinessDTO;
    }
}
