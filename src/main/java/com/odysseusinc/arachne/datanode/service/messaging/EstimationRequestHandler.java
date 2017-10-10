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
 * Created: July 18, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortAnalysisDTO;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.util.List;


//@Service
public class EstimationRequestHandler implements AtlasRequestHandler<CommonCohortAnalysisDTO, CommonCohortAnalysisDTO> {

    private final AtlasClient atlasClient;
    private final CentralSystemClient centralClient;

    public EstimationRequestHandler(AtlasClient atlasClient,
                                    CentralSystemClient centralClient) {

        this.atlasClient = atlasClient;
        this.centralClient = centralClient;
    }

    @Override
    public List<CommonCohortAnalysisDTO> getObjectsList() {

        return atlasClient.getAllEstimations();
    }

    @Override
    public CommonCohortAnalysisDTO getAtlasObject(String guid) {

        return atlasClient.getEstimation(guid);
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.ESTIMATION;
    }

    @Override
    public void sendResponse(CommonCohortAnalysisDTO response, String id) {

        centralClient.sendEstimationResponse(response, id);
    }
}
