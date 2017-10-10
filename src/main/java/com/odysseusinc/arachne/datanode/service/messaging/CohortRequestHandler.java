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
 * Created: July 17, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortShortDTO;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;


//@Service
public class CohortRequestHandler implements AtlasRequestHandler<CommonCohortShortDTO, CommonCohortDTO> {

    private final AtlasClient atlasClient;
    private final CentralSystemClient centralClient;

    @Autowired
    public CohortRequestHandler(AtlasClient atlasClient,
                                CentralSystemClient centralClient) {

        this.atlasClient = atlasClient;
        this.centralClient = centralClient;
    }


    @Override
    public List<CommonCohortShortDTO> getObjectsList() {

        return atlasClient.getAllCohorts();
    }

    @Override
    public CommonCohortDTO getAtlasObject(String guid) {

        return atlasClient.getCohort(guid);
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.COHORT;
    }

    @Override
    public void sendResponse(CommonCohortDTO response, String id) {

        centralClient.sendCohortResponse(response, id);
    }
}
