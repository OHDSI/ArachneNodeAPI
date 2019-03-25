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
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionInfo;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.messaging.prediction.PredictionAtlas2_5Mapper;
import com.odysseusinc.arachne.datanode.service.messaging.prediction.PredictionAtlas2_7Mapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PatientLevelPredictionRequestHandler extends BaseRequestHandler implements AtlasRequestHandler<CommonPredictionDTO, List<MultipartFile>> {

    private static final Logger logger = LoggerFactory.getLogger(PatientLevelPredictionRequestHandler.class);

    private final GenericConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private final CentralSystemClient centralClient;
    private final Template patientLevelPredictionRunnerTemplate;
    private final Template predictionRunnerTemplate;

    @Autowired
    public PatientLevelPredictionRequestHandler(AtlasService atlasService,
                                                GenericConversionService conversionService,
                                                CommonEntityService commonEntityService,
                                                CentralSystemClient centralClient,
                                                SqlRenderService sqlRenderService,
                                                @Qualifier("patientLevelPredictionRunnerTemplate")
                                                            Template patientLevelPredictionRunnerTemplate,
                                                @Qualifier("predictionRunnerTemplate") Template predictionRunnerTemplate) {

        super(sqlRenderService, atlasService);

        this.conversionService = conversionService;
        this.commonEntityService = commonEntityService;
        this.centralClient = centralClient;
        this.patientLevelPredictionRunnerTemplate = patientLevelPredictionRunnerTemplate;
        this.predictionRunnerTemplate = predictionRunnerTemplate;
    }

    @Override
    public List<CommonPredictionDTO> getObjectsList(List<Atlas> atlasList) {

        List<PatientLevelPredictionInfo> result = atlasService.execute(atlasList, AtlasClient::getPatientLevelPredictions);
        return result.stream()
                .map(plp -> conversionService.convert(plp, CommonPredictionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> getEntityMapper(entity).mapEntity(entity)).orElse(Collections.emptyList());
    }

    private EntityMapper<CommonEntity> getEntityMapper(CommonEntity entity) {
        Atlas atlas = entity.getOrigin();
        if (Constants.Atlas.ATLAS_2_7_VERSION.isLesserOrEqualsThan(atlas.getVersion())) {
            return new PredictionAtlas2_7Mapper(atlasService, predictionRunnerTemplate);
        } else {
            return new PredictionAtlas2_5Mapper(sqlRenderService, atlasService, patientLevelPredictionRunnerTemplate);
        }
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.PREDICTION;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[0]));
    }
}
