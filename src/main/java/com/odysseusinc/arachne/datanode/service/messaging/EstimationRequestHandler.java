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

package com.odysseusinc.arachne.datanode.service.messaging;

import static com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType.ESTIMATION;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortAnalysisDTO;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.messaging.estimation.EstimationAtlas2_5Mapper;
import com.odysseusinc.arachne.datanode.service.messaging.estimation.EstimationAtlas2_7Mapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class EstimationRequestHandler implements AtlasRequestHandler<CommonCohortAnalysisDTO, List<MultipartFile>> {

    protected final AtlasService atlasService;
    protected final CentralSystemClient centralClient;
    protected final GenericConversionService conversionService;
    protected final CommonEntityService commonEntityService;
    protected final Template runnerTemplate;
    protected final CohortExpressionQueryBuilder queryBuilder;
    protected final SqlRenderService sqlRenderService;
    protected final Template estimationRunnerTemplate;

    @Autowired
    public EstimationRequestHandler(AtlasService atlasService,
                                    CentralSystemClient centralClient,
                                    GenericConversionService conversionService,
                                    CommonEntityService commonEntityService,
                                    @Qualifier("estimationRunnerTemplate") Template runnerTemplate,
                                    CohortExpressionQueryBuilder queryBuilder,
                                    SqlRenderService sqlRenderService,
                                    @Qualifier("newEstimationRunnerTemplate") Template estimationRunnerTemplate) {

        this.atlasService = atlasService;
        this.centralClient = centralClient;
        this.conversionService = conversionService;
        this.commonEntityService = commonEntityService;
        this.runnerTemplate = runnerTemplate;
        this.queryBuilder = queryBuilder;
        this.sqlRenderService = sqlRenderService;
        this.estimationRunnerTemplate = estimationRunnerTemplate;
    }


    @Override
    public List<CommonCohortAnalysisDTO> getObjectsList(List<Atlas> atlasList) {

        List<? extends BaseAtlasEntity> analyses = atlasService.execute(atlasList, AtlasClient::getEstimations);
        return analyses
                .stream()
                .map(analysis -> conversionService.convert(analysis, CommonCohortAnalysisDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> getEntityMapper(entity).mapEntity(entity)).orElse(Collections.emptyList());
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return ESTIMATION;
    }

    protected EntityMapper<CommonEntity> getEntityMapper(CommonEntity entity) {

        Atlas atlas = entity.getOrigin();
        if (Constants.Atlas.ATLAS_2_7_VERSION.isLesserOrEqualsThan(atlas.getVersion())) {
            return new EstimationAtlas2_7Mapper(atlasService, estimationRunnerTemplate);
        } else {
            return new EstimationAtlas2_5Mapper(sqlRenderService, atlasService, queryBuilder, runnerTemplate);
        }
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[0]));
    }


}
