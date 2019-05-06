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
 * Authors: Pavel Grafkin
 * Created: April 30, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.ComparativeCohortAnalysis;
import org.springframework.core.convert.support.GenericConversionService;

public abstract class BaseCCAToCommonEntityDTOConverter<T extends CommonEntityDTO>
        extends BaseAtlasEntityToCommonEntityDTOConverter<ComparativeCohortAnalysis, T> {

    public BaseCCAToCommonEntityDTOConverter(GenericConversionService conversionService) {

        super(conversionService);
    }

    @Override
    public T convert(ComparativeCohortAnalysis source) {

        T result = super.convert(source);
        result.setLocalId(source.getAnalysisId().longValue());
        result.setModified(source.getModified());
        result.setType(CommonAnalysisType.ESTIMATION);
        return result;
    }

    @Override
    protected abstract T getTargetClass();
}
