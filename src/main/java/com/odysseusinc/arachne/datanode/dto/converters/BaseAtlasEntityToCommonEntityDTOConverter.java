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
 * Created: March 15, 2018
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

public abstract class BaseAtlasEntityToCommonEntityDTOConverter<S extends BaseAtlasEntity, T extends CommonEntityDTO>
        implements Converter<S, T>, InitializingBean {

    protected GenericConversionService conversionService;

    public BaseAtlasEntityToCommonEntityDTOConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public T convert(S source) {

        T result = getTargetClass();
        result.setOriginId(source.getOrigin().getId());
        result.setName(source.getName());
        return result;
    }

    protected abstract T getTargetClass();
}
