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

package com.odysseusinc.arachne.datanode.service.aspects;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityDTO;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;

import java.util.List;
import java.util.Objects;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



@Component
@Aspect
public class CommonEntityAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonEntityAspect.class);
    private final CommonEntityService commonEntityService;

    @Autowired
    public CommonEntityAspect(CommonEntityService commonEntityService) {

        this.commonEntityService = commonEntityService;
    }

    @AfterReturning(
            pointcut = "execution(* com.odysseusinc.arachne.datanode.service.AtlasRequestHandler.getObjectsList(..))",
            returning = "entityList"
    )
    public void expandListWithGuid(List<? extends CommonEntityDTO> entityList) {

        entityList.forEach(this::expandWithGuid);
    }

    private void expandWithGuid(CommonEntityDTO commonEntityDTO) {

        Objects.requireNonNull(commonEntityDTO);
        if (commonEntityDTO.getType() != null) {
            CommonEntity entity = commonEntityService.getOrCreate(commonEntityDTO.getOriginId(), commonEntityDTO.getLocalId().intValue(),
                    commonEntityDTO.getType());
            commonEntityDTO.setGuid(entity.getGuid());
        }
    }
}
