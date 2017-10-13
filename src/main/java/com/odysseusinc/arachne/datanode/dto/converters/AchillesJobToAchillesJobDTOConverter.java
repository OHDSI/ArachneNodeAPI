/**
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
 * Created: June 08, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.datanode.dto.achilles.AchillesJobDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class AchillesJobToAchillesJobDTOConverter implements Converter<AchillesJob, AchillesJobDTO> {

    private GenericConversionService conversionService;

    @Autowired
    public AchillesJobToAchillesJobDTOConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;
        conversionService.addConverter(this);
    }

    @Override
    public AchillesJobDTO convert(AchillesJob achillesJob) {

        AchillesJobDTO dto = new AchillesJobDTO();
        dto.setDataSource(conversionService.convert(achillesJob.getDataSource(), DataSourceDTO.class));
        dto.setStarted(achillesJob.getStarted());
        dto.setFinished(achillesJob.getFinished());
        dto.setStatus(achillesJob.getStatus());
        dto.setSource(achillesJob.getSource());
        return dto;
    }
}
