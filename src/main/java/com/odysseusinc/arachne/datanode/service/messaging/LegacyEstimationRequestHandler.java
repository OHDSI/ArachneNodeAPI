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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: July 27, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;

import static com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType.ESTIMATION;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCohortAnalysisDTO;
import com.odysseusinc.arachne.commons.utils.CommonFileUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.dto.atlas.ComparativeCohortAnalysis;
import com.odysseusinc.arachne.datanode.dto.atlas.ComparativeCohortAnalysisInfo;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.CohortService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.io.IOUtils;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class LegacyEstimationRequestHandler implements AtlasRequestHandler<CommonCohortAnalysisDTO, List<MultipartFile>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyEstimationRequestHandler.class);
    private final AtlasClient atlasClient;
    private final CentralSystemClient centralClient;
    private final GenericConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private final Template runnerTemplate;
    private final CohortExpressionQueryBuilder queryBuilder;

    @Autowired
    public LegacyEstimationRequestHandler(AtlasClient atlasClient,
                                          CentralSystemClient centralClient,
                                          GenericConversionService conversionService,
                                          CommonEntityService commonEntityService,
                                          @Qualifier("estimationRunnerTemplate") Template runnerTemplate,
                                          CohortExpressionQueryBuilder queryBuilder) {

        this.atlasClient = atlasClient;
        this.centralClient = centralClient;
        this.conversionService = conversionService;
        this.commonEntityService = commonEntityService;
        this.runnerTemplate = runnerTemplate;
        this.queryBuilder = queryBuilder;
    }


    @Override
    public List<CommonCohortAnalysisDTO> getObjectsList() {

        List<ComparativeCohortAnalysis> analyses = atlasClient.getComparativeCohortAnalyses();
        return analyses
                .stream()
                .map(analysis -> conversionService.convert(analysis, CommonCohortAnalysisDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        final List<MultipartFile> result = new ArrayList<>();

        commonEntityService.findByGuid(guid).ifPresent(entity -> {
            ComparativeCohortAnalysisInfo analysis = atlasClient.getComparativeCohortAnalysisInfo(entity.getLocalId());
            if (analysis != null) {
                try {
                    String name = analysis.getName().trim();

                    String estimationJson = buildEstimationDesign(analysis);
                    result.add(new MockMultipartFile(getEstimationFilename(name), estimationJson.getBytes()));

                    String targetCohortSql = getCohortSql(analysis.getTreatmentId());
                    result.add(new MockMultipartFile(getTargetCohortFilename(name), targetCohortSql.getBytes()));

                    String comparatorCohortSql = getCohortSql(analysis.getComparatorId());
                    result.add(new MockMultipartFile(getComparatorCohortFilename(name), comparatorCohortSql.getBytes()));

                    String outcomeCohortSql = getCohortSql(analysis.getOutcomeId());
                    result.add(new MockMultipartFile(getOutcomeCohortFilename(name), outcomeCohortSql.getBytes()));

                    String runnerR = buildRunner(name);
                    result.add(new MockMultipartFile("main.r", runnerR.getBytes()));

                } catch (IOException | NoSuchMethodException | ScriptException e) {
                    LOGGER.error("Failed to construct estimation", e);
                }
            }
        });

        return result;
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return ESTIMATION;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[response.size()]));
    }

    private String buildEstimationDesign(ComparativeCohortAnalysisInfo info)
            throws IOException, ScriptException, NoSuchMethodException {

        ObjectMapper mapper = new ObjectMapper();
        String infoJson = mapper.writeValueAsString(info);

        Resource jsResource = new ClassPathResource("estimation/EstimationBuilder.js");
        InputStream jsResourceStream = jsResource.getInputStream();
        String jsCode = IOUtils.toString(jsResourceStream, "UTF-8");
        jsResourceStream.close();

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(jsCode);

        Invocable invocable = (Invocable) engine;
        Object result = invocable.invokeFunction("build", infoJson);

        return (String) result;
    }

    private String buildRunner(String analysisName) throws IOException {

        Map<String, Object> parameters = getRunnerParams(analysisName);

        return runnerTemplate.apply(parameters);
    }

    protected Map<String, Object> getRunnerParams(String analysisName) {

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("analysisFile", getEstimationFilename(analysisName));
        parameters.put("targetCohort", getTargetCohortFilename(analysisName));
        parameters.put("comparatorCohort", getComparatorCohortFilename(analysisName));
        parameters.put("outcomeCohort", getOutcomeCohortFilename(analysisName));

        return parameters;
    }

    private String getEstimationFilename(String analysisName) {

        return analysisName + CommonFileUtils.ESTIMATION_EXT;
    }

    private String getTargetCohortFilename(String analysisName) {

        return getCohortFilename(combineName(analysisName, "target"));
    }

    private String getComparatorCohortFilename(String analysisName) {

        return getCohortFilename(combineName(analysisName, "comparator"));
    }

    private String getOutcomeCohortFilename(String analysisName) {

        return getCohortFilename(combineName(analysisName, "outcome"));
    }

    private String getCohortFilename(String cohortName) {

        return cohortName + CommonFileUtils.OHDSI_SQL_EXT;
    }

    private String combineName(String prefix, String name) {

        return prefix + "_" + name;
    }

    private String getCohortSql(Integer cohortId) throws IOException {

        CohortDefinition definition = atlasClient.getCohortDefinition(cohortId);
        ObjectMapper mapper = new ObjectMapper();
        CohortExpression expression = mapper.readValue(definition.getExpression(), CohortExpression.class);
        final CohortExpressionQueryBuilder.BuildExpressionQueryOptions options
                = new CohortExpressionQueryBuilder.BuildExpressionQueryOptions();
        return CohortService.IGNORE_PREPROCESSING_MARK
                + "\r\n"
                + queryBuilder.buildExpressionQuery(expression, options);
    }
}
