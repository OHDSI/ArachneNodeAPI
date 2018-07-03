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
 * Authors: Pavel Grafkin
 * Created: March 14, 2018
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.datanode.dto.atlas.AtlasDetailedDTO;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthSchema;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class AtlasDetailedDTOToAtlasConverter implements Converter<AtlasDetailedDTO, Atlas>, InitializingBean {

    private GenericConversionService conversionService;

    @Autowired
    public AtlasDetailedDTOToAtlasConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public Atlas convert(AtlasDetailedDTO source) {

        Atlas result = new Atlas();

        result.setId(null);
        result.setCentralId(null);
        result.setName(source.getName());
        result.setVersion(null);

        result.setUrl(source.getUrl());
        result.setAuthType(AtlasAuthSchema.valueOf(source.getAuthType()));
        result.setUsername(source.getUsername());
        result.setPassword(source.getPassword());

        return result;
    }
}
