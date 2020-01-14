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

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonEstimationDTO;
import com.odysseusinc.arachne.commons.utils.ConverterUtils;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AnalysisInfoBuilder;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.messaging.estimation.EstimationAtlas2_5Mapper;
import com.odysseusinc.arachne.datanode.service.messaging.estimation.EstimationAtlas2_7Mapper;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType.ESTIMATION;


@Service
public class EstimationRequestHandler extends CommonAnalysisRequestHandler implements AtlasRequestHandler<CommonEstimationDTO, List<MultipartFile>> {

    protected final AnalysisInfoBuilder analysisInfoBuilder;
    protected final AtlasService atlasService;
    protected final CentralSystemClient centralClient;
    protected final CommonEntityService commonEntityService;
    protected final Template runnerTemplate;
    protected final CohortExpressionQueryBuilder queryBuilder;
    protected final SqlRenderService sqlRenderService;
    protected final Template estimationRunnerTemplate;
    protected final ConverterUtils converterUtils;

    @Autowired
    public EstimationRequestHandler(AnalysisInfoBuilder analysisInfoBuilder,
                                    AtlasService atlasService,
                                    CentralSystemClient centralClient,
                                    CommonEntityService commonEntityService,
                                    @Qualifier("estimationRunnerTemplate") Template legacyRunnerTemplate,
                                    CohortExpressionQueryBuilder queryBuilder,
                                    SqlRenderService sqlRenderService,
                                    @Qualifier("newEstimationRunnerTemplate") Template estimationRunnerTemplate,
                                    ConverterUtils converterUtils) {

        super(sqlRenderService, atlasService, estimationRunnerTemplate, legacyRunnerTemplate, centralClient);

        this.atlasService = atlasService;
        this.analysisInfoBuilder = analysisInfoBuilder;
        this.centralClient = centralClient;
        this.commonEntityService = commonEntityService;
        this.runnerTemplate = legacyRunnerTemplate;
        this.queryBuilder = queryBuilder;
        this.sqlRenderService = sqlRenderService;
        this.estimationRunnerTemplate = estimationRunnerTemplate;
        this.converterUtils = converterUtils;
    }


    @Override
    public List<CommonEstimationDTO> getObjectsList(List<Atlas> atlasList) {

        return converterUtils.convertList(getEntities(atlasList), CommonEstimationDTO.class);
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> getEntityMapper(entity.getOrigin())
                .mapEntity(entity)).orElse(Collections.emptyList());
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return ESTIMATION;
    }

    protected <T extends BaseAtlasEntity, C extends AtlasClient> EntityMapper<T, CommonEntity, C> getEntityMapper(Atlas atlas) {

        if (Constants.Atlas.ATLAS_2_7_VERSION.isLesserOrEqualsThan(atlas.getVersion())) {
            return (EntityMapper<T, CommonEntity, C>) new EstimationAtlas2_7Mapper(atlasService, runnerTemplate, analysisInfoBuilder);
        } else {
            return (EntityMapper<T, CommonEntity, C>) new EstimationAtlas2_5Mapper(sqlRenderService, atlasService, queryBuilder, legacyRunnerTemplate);
        }
    }


}
