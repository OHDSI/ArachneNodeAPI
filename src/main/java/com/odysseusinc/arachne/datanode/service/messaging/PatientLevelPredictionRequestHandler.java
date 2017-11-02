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
 * Created: Nov 1, 2017
 *
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonPredictionDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionAnalysisInfo;
import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionInfo;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientLevelPredictionRequestHandler implements AtlasRequestHandler<CommonPredictionDTO, MultipartFile> {

    private final AtlasClient atlasClient;
    private final GenericConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private final CentralSystemClient centralClient;

    @Autowired
    public PatientLevelPredictionRequestHandler(AtlasClient atlasClient,
                                                GenericConversionService conversionService,
                                                CommonEntityService commonEntityService,
                                                CentralSystemClient centralClient) {

        this.atlasClient = atlasClient;
        this.conversionService = conversionService;
        this.commonEntityService = commonEntityService;
        this.centralClient = centralClient;
    }

    @Override
    public List<CommonPredictionDTO> getObjectsList() {

        List<PatientLevelPredictionInfo> result = atlasClient.getPatientLevelPredictions();
        return result.stream()
                .map(plp -> conversionService.convert(plp, CommonPredictionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public MultipartFile getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> {
            PatientLevelPredictionAnalysisInfo info = atlasClient.getPatientLevelPrediction(entity.getLocalId());
            return new MockMultipartFile("prediction.r", new byte[0]);
        }).orElse(null);
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.PREDICTION;
    }

    @Override
    public void sendResponse(MultipartFile response, String id) {

        MultipartFile[] files = new MultipartFile[]{ response };
        centralClient.sendCommonEntityResponse(id, files);
    }
}
