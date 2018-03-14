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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Nov 3, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonIncidenceRatesDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.IRAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class IRAnalysisToCommonIncidenceRatesDTOConverter implements Converter<IRAnalysis, CommonIncidenceRatesDTO> {

    @Autowired
    public IRAnalysisToCommonIncidenceRatesDTOConverter(GenericConversionService conversionService) {

        conversionService.addConverter(this);
    }

    @Override
    public CommonIncidenceRatesDTO convert(IRAnalysis source) {

        CommonIncidenceRatesDTO result = new CommonIncidenceRatesDTO();
        result.setOriginId(source.getOrigin().getId());
        result.setDescription(source.getDescription());
        result.setLocalId(source.getId().longValue());
        result.setName(source.getName());
        result.setModified(source.getModifiedDate());
        result.setType(CommonAnalysisType.INCIDENCE);
        return result;
    }
}
