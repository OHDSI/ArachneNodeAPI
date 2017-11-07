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

package com.odysseusinc.arachne.datanode.controller;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceBusinessDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.service.CentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import com.odysseusinc.arachne.datanode.service.UserService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.datanode.service.impl.DataSourceHelperImpl;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataSourceController extends BaseDataSourceController<DataSource, DataSourceBusinessDTO, CommonDataSourceDTO> {

    private static final String CDM_VERSION_FILENAME = "cdm_version.txt";

    @Autowired
    public DataSourceController(DataSourceService dataSourceService,
                                CentralIntegrationService integrationService,
                                UserService userService,
                                ModelMapper modelMapper,
                                GenericConversionService conversionService,
                                JmsTemplate jmsTemplate,
                                DataSourceHelperImpl dataSourceHelper,
                                CentralClient centralClient) {

        super(userService,
                modelMapper,
                integrationService,
                dataSourceService,
                conversionService,
                dataSourceHelper,
                centralClient,
                jmsTemplate);
    }

    @Override
    protected Class getCommonDataSourceDTOClass() {

        return CommonDataSourceDTO.class;
    }

    @Override
    protected Class<? extends DataSourceBusinessDTO> getDataSourceBusinessDTOClass() {

        return DataSourceBusinessDTO.class;
    }

    @Override
    protected DataSourceBusinessDTO enrichBusinessFromCommon(DataSourceBusinessDTO businessDTO, CommonDataSourceDTO commonDataSourceDTO) {

        businessDTO.setId(commonDataSourceDTO.getId());
        businessDTO.setUuid(commonDataSourceDTO.getUuid());
        businessDTO.setName(commonDataSourceDTO.getName());
        businessDTO.setModelType(commonDataSourceDTO.getModelType());
        businessDTO.setOrganization(commonDataSourceDTO.getOrganization());
        businessDTO.setCdmVersion(commonDataSourceDTO.getCdmVersion());
        return businessDTO;
    }
}
