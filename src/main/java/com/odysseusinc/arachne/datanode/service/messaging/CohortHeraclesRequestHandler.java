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
 * Created: Nov 24, 2017
 *
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;


import static com.odysseusinc.arachne.commons.utils.CommonFileUtils.ANALYSIS_INFO_FILE_DESCRIPTION;
import static com.odysseusinc.arachne.datanode.service.messaging.MessagingUtils.ignorePreprocessingMark;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortShortDTO;
import com.odysseusinc.arachne.commons.utils.CommonFileUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AnalysisInfoBuilder;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CohortHeraclesRequestHandler implements AtlasRequestHandler<CommonCohortShortDTO, List<MultipartFile>> {

    private static final Logger logger = LoggerFactory.getLogger(CohortHeraclesRequestHandler.class);

    private final AtlasService atlasService;
    private final AnalysisInfoBuilder analysisInfoBuilder;
    private final GenericConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private final CentralSystemClient centralClient;
    private final SqlRenderService sqlRenderService;
    protected final Template runnerTemplate;

    @Value("${cohorts.result.countEnabled}")
    private Boolean countEnabled;
    @Value("${cohorts.result.summaryEnabled}")
    private Boolean summaryEnabled;

    @Autowired
    public CohortHeraclesRequestHandler(AtlasService atlasService,
                                        AnalysisInfoBuilder analysisInfoBuilder,
                                        GenericConversionService conversionService,
                                        CommonEntityService commonEntityService,
                                        CentralSystemClient centralClient,
                                        SqlRenderService sqlRenderService,
                                        @Qualifier("cohortHeraclesRunnerTemplate")
                                                        Template runnerTemplate) {

        this.atlasService = atlasService;
        this.analysisInfoBuilder = analysisInfoBuilder;
        this.conversionService = conversionService;
        this.commonEntityService = commonEntityService;
        this.centralClient = centralClient;
        this.sqlRenderService = sqlRenderService;
        this.runnerTemplate = runnerTemplate;
    }

    @Override
    public List<CommonCohortShortDTO> getObjectsList(List<Atlas> atlasList) {

        List<CohortDefinition> definitions = atlasService.execute(atlasList, AtlasClient::getCohortDefinitions);
        return definitions
                .stream()
                .map(cohort -> conversionService.convert(cohort, CommonCohortShortDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService
                .findByGuid(guid).map(this::buildCohortAnalysisFileList)
                .orElse(Collections.emptyList());
    }

    private MockMultipartFile generateFile(String resourcePath, String outputName) throws IOException {

        ClassPathResource resource = new ClassPathResource(resourcePath);
        final String resourceContent = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());
        return new MockMultipartFile("file", outputName, MediaType.APPLICATION_OCTET_STREAM_VALUE, ignorePreprocessingMark(resourceContent).getBytes());
    }

    private MultipartFile getRunner(String initialFileName) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("initialFileName", initialFileName);
        String result = runnerTemplate.apply(params);
        return new MockMultipartFile("file", "main.r", MediaType.APPLICATION_OCTET_STREAM_VALUE, result.getBytes());
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.COHORT_HERACLES;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[response.size()]));
    }

    private List<MultipartFile> buildCohortAnalysisFileList(CommonEntity cohortEntity) {

        logger.debug("Generating Cohort analysis files for {} : {}", cohortEntity.getAnalysisType().getTitle(), cohortEntity.getId());
        CohortDefinition cohortDefinition = atlasService.execute(cohortEntity.getOrigin(), atlasClient -> atlasClient.getCohortDefinition(cohortEntity.getLocalId()));
        if (Objects.nonNull(cohortDefinition)) {
            String definitionSql = sqlRenderService.renderSql(cohortDefinition);
            if (Objects.nonNull(definitionSql)) {
                List<MultipartFile> files = new ArrayList<>(3);
                String cohortSqlFileName = cohortDefinition.getName().trim() + CommonFileUtils.OHDSI_SQL_EXT;
                final byte[] cohordDefinitionBytes = ignorePreprocessingMark(definitionSql).getBytes();
                files.add(new MockMultipartFile("file", cohortSqlFileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, cohordDefinitionBytes));
                String description = analysisInfoBuilder.generateHeraclesAnalysisDescription(cohortDefinition);
                files.add(new MockMultipartFile("file", ANALYSIS_INFO_FILE_DESCRIPTION, MediaType.TEXT_PLAIN_VALUE, description.getBytes()));
                try {
                    files.add(getRunner(cohortSqlFileName));
                    if (countEnabled) {
                        files.add(generateFile("cohort/cohort-count.sql", "cohort-count.ohdsi.sql"));
                    }
                    if (summaryEnabled) {
                        files.add(generateFile("cohort/cohort-summary.sql", "cohort-summary.ohdsi.sql"));
                    }
                    return files;
                } catch (IOException e) {
                    logger.error("Failed to build Cohort Analyses data", e);
                    throw new UncheckedIOException("Failed to build Cohort Analyses data", e);
                }
            }
        }
        return Collections.emptyList();
    }

}
