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

package com.odysseusinc.arachne.datanode.service.messaging;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonIncidenceRatesDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.IRAnalysis;
import com.odysseusinc.arachne.datanode.dto.atlas.IRAnalysisInfo;
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
public class IncidenceRatesRequestHandler implements AtlasRequestHandler<CommonIncidenceRatesDTO, MultipartFile> {

    private final AtlasClient atlasClient;
    private final GenericConversionService conversionService;
    private final CentralSystemClient centralClient;
    private final CommonEntityService commonEntityService;

    @Autowired
    public IncidenceRatesRequestHandler(AtlasClient atlasClient,
                                        GenericConversionService conversionService,
                                        CentralSystemClient centralClient,
                                        CommonEntityService commonEntityService) {

        this.atlasClient = atlasClient;
        this.conversionService = conversionService;
        this.centralClient = centralClient;
        this.commonEntityService = commonEntityService;
    }

    @Override
    public List<CommonIncidenceRatesDTO> getObjectsList() {

        List<IRAnalysis> irAnalyses = atlasClient.getIncidenceRates();
        return irAnalyses.stream()
                .map(ir -> conversionService.convert(ir, CommonIncidenceRatesDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public MultipartFile getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> {
            IRAnalysisInfo analysis = atlasClient.getIncidenceRate(entity.getLocalId());
            return new MockMultipartFile("ir_analysis.r", new byte[0]);
        }).orElse(null);
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.INCIDENCE;
    }

    @Override
    public void sendResponse(MultipartFile response, String id) {

        MultipartFile[] files = new MultipartFile[]{ response };
        centralClient.sendCommonEntityResponse(id, files);
    }
}
