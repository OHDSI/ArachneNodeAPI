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

import com.odysseusinc.arachne.datanode.dto.atlas.AtlasDTO;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import org.springframework.core.convert.support.GenericConversionService;

public abstract class BaseAtlasToAtlasDTOConverter<T extends AtlasDTO> extends BaseAtlasToAtlasShortDTOConverter<T> {

    public BaseAtlasToAtlasDTOConverter(GenericConversionService conversionService) {

        super(conversionService);
    }

    @Override
    public T convert(Atlas source) {

        T result = super.convert(source);

        result.setId(source.getId());
        result.setUrl(source.getUrl());

        return result;
    }
}
