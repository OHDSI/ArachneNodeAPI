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
 * Created: July 26, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import static com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType.COHORT;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortShortDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class CohortDefinitionToCommonCohortShortDTOConverter
        extends BaseAtlasEntityToCommonEntityDTOConverter<CohortDefinition, CommonCohortShortDTO> {

    public CohortDefinitionToCommonCohortShortDTOConverter(GenericConversionService conversionService) {

        super(conversionService);
    }

    @Override
    public CommonCohortShortDTO convert(CohortDefinition source) {

        CommonCohortShortDTO cohort = super.convert(source);
        cohort.setLocalId(source.getId());
        cohort.setDescription(source.getDescription());
        cohort.setModified(source.getModifiedDate());
        cohort.setType(COHORT);
        return cohort;
    }

    @Override
    protected CommonCohortShortDTO getTargetClass() {

        return new CommonCohortShortDTO();
    }
}
