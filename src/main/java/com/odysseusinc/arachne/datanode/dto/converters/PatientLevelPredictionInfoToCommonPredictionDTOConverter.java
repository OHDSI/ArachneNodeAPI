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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Nov 1, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonPredictionDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionInfo;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class PatientLevelPredictionInfoToCommonPredictionDTOConverter
        extends BaseAtlasEntityToCommonEntityDTOConverter<PatientLevelPredictionInfo, CommonPredictionDTO> {

    public PatientLevelPredictionInfoToCommonPredictionDTOConverter(GenericConversionService conversionService) {

        super(conversionService);
    }

    @Override
    public CommonPredictionDTO convert(PatientLevelPredictionInfo source) {

        CommonPredictionDTO dto = super.convert(source);
        dto.setLocalId(source.getAnalysisId().longValue());
        dto.setModified(source.getModifiedDate());
        dto.setType(CommonAnalysisType.PREDICTION);
        return dto;
    }

    @Override
    protected CommonPredictionDTO getTargetClass() {

        return new CommonPredictionDTO();
    }
}
