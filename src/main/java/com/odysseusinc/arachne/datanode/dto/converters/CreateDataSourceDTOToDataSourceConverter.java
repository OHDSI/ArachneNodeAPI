/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
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
import com.odysseusinc.arachne.datanode.dto.datasource.CreateDataSourceDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import java.io.IOException;
import java.util.Objects;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class CreateDataSourceDTOToDataSourceConverter implements Converter<CreateDataSourceDTO, DataSource>, InitializingBean {

    private GenericConversionService conversionService;

    @Autowired
    public CreateDataSourceDTOToDataSourceConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public DataSource convert(CreateDataSourceDTO createDataSourceDTO) {

        if (createDataSourceDTO == null) {
            return null;
        }
        DataSource dataSource = new DataSource();
        dataSource.setName(createDataSourceDTO.getName());
        dataSource.setDescription(createDataSourceDTO.getDescription());
        dataSource.setType(DBMSType.valueOf(createDataSourceDTO.getDbmsType()));
        dataSource.setConnectionString(createDataSourceDTO.getConnectionString());
        dataSource.setUsername(createDataSourceDTO.getDbUsername());
        dataSource.setPassword(createDataSourceDTO.getDbPassword());
        dataSource.setCdmSchema(createDataSourceDTO.getCdmSchema());
        dataSource.setCohortTargetTable(createDataSourceDTO.getCohortTargetTable());
        dataSource.setTargetSchema(createDataSourceDTO.getTargetSchema());
        dataSource.setResultSchema(createDataSourceDTO.getResultSchema());
        dataSource.setUseKerberos(createDataSourceDTO.getUseKerberos());
        dataSource.setKrbFQDN(createDataSourceDTO.getKrbFQDN());
        dataSource.setKrbRealm(createDataSourceDTO.getKrbRealm());
        dataSource.setKrbUser(createDataSourceDTO.getKrbUser());
        dataSource.setKrbPassword(createDataSourceDTO.getKrbPassword());
        dataSource.setKrbAuthMethod(createDataSourceDTO.getKrbAuthMethod());
        if (Objects.nonNull(createDataSourceDTO.getKrbKeytab())) {
            try {
                dataSource.setKrbKeytab(createDataSourceDTO.getKrbKeytab().getBytes());
            } catch (IOException ignored) {
            }
        }
        return dataSource;

    }

}
