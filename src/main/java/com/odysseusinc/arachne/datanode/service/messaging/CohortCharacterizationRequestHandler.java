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
 * Created: Nov 24, 2017
 *
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;


import static com.odysseusinc.arachne.datanode.service.messaging.MessagingUtils.ignorePreprocessingMark;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortShortDTO;
import com.odysseusinc.arachne.commons.utils.CommonFileUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CohortCharacterizationRequestHandler implements AtlasRequestHandler<CommonCohortShortDTO, List<MultipartFile>> {

    private static final Logger logger = LoggerFactory.getLogger(CohortCharacterizationRequestHandler.class);

    private final AtlasClient atlasClient;
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
    public CohortCharacterizationRequestHandler(AtlasClient atlasClient,
                                                GenericConversionService conversionService,
                                                CommonEntityService commonEntityService,
                                                CentralSystemClient centralClient,
                                                SqlRenderService sqlRenderService,
                                                @Qualifier("cohortCharacterizationRunnerTemplate")
                                                        Template runnerTemplate) {

        this.atlasClient = atlasClient;
        this.conversionService = conversionService;
        this.commonEntityService = commonEntityService;
        this.centralClient = centralClient;
        this.sqlRenderService = sqlRenderService;
        this.runnerTemplate = runnerTemplate;
    }

    @Override
    public List<CommonCohortShortDTO> getObjectsList() {

        List<CohortDefinition> definitions = atlasClient.getCohortDefinitions();
        return definitions
                .stream()
                .map(cohort -> conversionService.convert(cohort, CommonCohortShortDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> {
            List<MultipartFile> files = new ArrayList<>(2);

            CohortDefinition definition = atlasClient.getCohortDefinition(entity.getLocalId());
            if (Objects.nonNull(definition)) {
                String content = sqlRenderService.renderSql(definition);
                if (Objects.nonNull(content)) {
                    String cohortSqlFileName = definition.getName().trim() + CommonFileUtils.OHDSI_SQL_EXT;
                    files.add(new MockMultipartFile(cohortSqlFileName, ignorePreprocessingMark(content).getBytes()));
                    try {
                        files.add(getRunner(cohortSqlFileName));
                        if (countEnabled) {
                            files.add(generateFile("cohort/cohort-count.sql", "cohort-count.ohdsi.sql"));
                        }
                        if (summaryEnabled) {
                            files.add(generateFile("cohort/cohort-summary.sql", "cohort-summary.ohdsi.sql"));
                        }
                    } catch (IOException e) {
                        logger.error("Failed to build CC data", e);
                        throw new RuntimeIOException("Failed to build CC data", e);
                    }

                } else {
                    return null;
                }
            }
            return files;
        }).orElse(null);
    }

    private MockMultipartFile generateFile(String resourcePath, String outputName) throws IOException {

        ClassPathResource resource = new ClassPathResource(resourcePath);
        final String resourceContent = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());
        return new MockMultipartFile(outputName, ignorePreprocessingMark(resourceContent).getBytes());
    }

    private MultipartFile getRunner(String initialFileName) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("initialFileName", initialFileName);
        String result = runnerTemplate.apply(params);
        return new MockMultipartFile("main.r", result.getBytes());
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.COHORT_CHARACTERIZATION;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[response.size()]));
    }
}
