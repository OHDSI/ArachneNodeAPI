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
 * Created: July 27, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortAnalysisDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortAnalysisType;
import com.odysseusinc.arachne.datanode.dto.atlas.ComparativeCohortAnalysis;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class CCAToCommonCohortAnalysisDTOConverter
        extends BaseCCAToCommonEntityDTOConverter<CommonCohortAnalysisDTO> {

    public CCAToCommonCohortAnalysisDTOConverter(GenericConversionService conversionService) {

        super(conversionService);
    }

    @Override
    public CommonCohortAnalysisDTO convert(ComparativeCohortAnalysis source) {

        CommonCohortAnalysisDTO result = super.convert(source);
        result.setAnalysisType(CommonCohortAnalysisType.ESTIMATION);
        return result;
    }

    @Override
    protected CommonCohortAnalysisDTO getTargetClass() {

        return new CommonCohortAnalysisDTO();
    }
}
