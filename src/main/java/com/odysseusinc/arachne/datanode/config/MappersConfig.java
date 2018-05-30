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
 * Created: November 18, 2016
 *
 */

package com.odysseusinc.arachne.datanode.config;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MappersConfig {
    @Bean
    public ModelMapper mapper() {

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.addMappings(getUserMapper());
        modelMapper.addMappings(getDataSourceMapper());
        modelMapper.addMappings(getDataSourceToRegisterDataSourceDTOMapper());

        return modelMapper;
    }

    private PropertyMap getUserMapper() {

        return new PropertyMap<User, CommonUserDTO>() {
            @Override
            protected void configure() {

                map(source.getId(), destination.getId());
                map(source.getFirstName(), destination.getFirstname());
                map(source.getLastName(), destination.getLastname());
                map(source.getEmail(), destination.getEmail());
            }
        };
    }

    private PropertyMap getDataSourceMapper() {

        return new PropertyMap<DataSource, DataSourceDTO>() {

            @Override
            protected void configure() {

                map(source.getId(), destination.getId());
                map(source.getName(), destination.getName());
                map(source.getDescription(), destination.getDescription());
                map(source.getConnectionString(), destination.getConnectionString());
                map(source.getType(), destination.getDbmsType());
                map(source.getCdmSchema(), destination.getCdmSchema());
                map(source.getUsername(), destination.getDbUsername());
                map(source.getPassword(), destination.getDbPassword());
                map(source.getHealthStatus(), destination.getHealthStatus());
                map(source.getHealthStatusDescription(), destination.getHealthStatusDescription());
            }
        };
    }

    private PropertyMap getDataSourceToRegisterDataSourceDTOMapper() {

        return new PropertyMap<DataSource, CommonDataSourceDTO>() {

            @Override
            protected void configure() {

                map(source.getName(), destination.getName());
            }
        };
    }
}
