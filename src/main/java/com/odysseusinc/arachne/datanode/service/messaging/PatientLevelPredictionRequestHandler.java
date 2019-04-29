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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Nov 1, 2017
 *
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonPredictionDTO;
import com.odysseusinc.arachne.commons.utils.ConverterUtils;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PatientLevelPredictionRequestHandler extends CommonAnalysisRequestHandler implements AtlasRequestHandler<CommonPredictionDTO, List<MultipartFile>> {

    private static final Logger logger = LoggerFactory.getLogger(PatientLevelPredictionRequestHandler.class);

    private final CommonEntityService commonEntityService;
    private final ConverterUtils converterUtils;

    @Autowired
    public PatientLevelPredictionRequestHandler(AtlasService atlasService,
                                                CommonEntityService commonEntityService,
                                                CentralSystemClient centralClient,
                                                SqlRenderService sqlRenderService,
                                                @Qualifier("patientLevelPredictionRunnerTemplate")
                                                            Template patientLevelPredictionRunnerTemplate,
                                                @Qualifier("predictionRunnerTemplate") Template predictionRunnerTemplate,
                                                ConverterUtils converterUtils) {

        super(sqlRenderService, atlasService, predictionRunnerTemplate, patientLevelPredictionRunnerTemplate, centralClient);

        this.commonEntityService = commonEntityService;
        this.converterUtils = converterUtils;
    }

    @Override
    public List<CommonPredictionDTO> getObjectsList(List<Atlas> atlasList) {

        return converterUtils.convertList(getEntities(atlasList), CommonPredictionDTO.class);
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> getEntityMapper(entity.getOrigin())
                .mapEntity(entity)).orElse(Collections.emptyList());
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.PREDICTION;
    }

}
