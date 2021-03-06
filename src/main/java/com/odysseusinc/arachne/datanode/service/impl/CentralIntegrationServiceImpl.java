/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
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

package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.service.CentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.util.CentralUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;

@Service
public class CentralIntegrationServiceImpl extends BaseCentralIntegrationServiceImpl<DataSource, CommonDataSourceDTO>
        implements CentralIntegrationService {

    @Autowired
    public CentralIntegrationServiceImpl(
            GenericConversionService conversionService,
            CentralUtil centralUtil,
            ApplicationContext applicationContext) {

        super(conversionService, centralUtil, applicationContext);
    }

}
