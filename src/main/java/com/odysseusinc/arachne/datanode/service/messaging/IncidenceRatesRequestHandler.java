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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonIncidenceRatesDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.IRAnalysis;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class IncidenceRatesRequestHandler extends BaseRequestHandler implements AtlasRequestHandler<CommonIncidenceRatesDTO, List<MultipartFile>> {

    private static final Logger logger = LoggerFactory.getLogger(IncidenceRatesRequestHandler.class);
    public static final String IR_BUILD_ERROR = "Failed to build IR data";
    public static final String ID_PROPERTY_SUFFIX = "Ids";
    private final AtlasClient atlasClient;
    private final GenericConversionService conversionService;
    private final CentralSystemClient centralClient;
    private final CommonEntityService commonEntityService;
    private Template incidenceRatesTemplate;

    @Autowired
    public IncidenceRatesRequestHandler(AtlasClient atlasClient,
                                        GenericConversionService conversionService,
                                        CentralSystemClient centralClient,
                                        CommonEntityService commonEntityService,
                                        SqlRenderService sqlRenderService,
                                        @Qualifier("incidenceRatesRunnerTemplate")
                                        Template incidenceRatesTemplate) {

        super(sqlRenderService, atlasClient);
        this.atlasClient = atlasClient;
        this.conversionService = conversionService;
        this.centralClient = centralClient;
        this.commonEntityService = commonEntityService;
        this.incidenceRatesTemplate = incidenceRatesTemplate;
    }

    @Override
    public List<CommonIncidenceRatesDTO> getObjectsList() {

        List<IRAnalysis> irAnalyses = atlasClient.getIncidenceRates();
        return irAnalyses.stream()
                .map(ir -> conversionService.convert(ir, CommonIncidenceRatesDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> {
            Map<String, Object> analysis = atlasClient.getIncidenceRate(entity.getLocalId());
            String analysisName = (String) analysis.getOrDefault("name", "ir_analysis");
            List<MultipartFile> files = new ArrayList<>();
            try {
                String expressionValue = (String) analysis.getOrDefault("expression", "");
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> expression = mapper.readValue(expressionValue, Map.class);
                files.add(getAnalysisDescription(analysis));
                List<MultipartFile> cohortFiles = new LinkedList<>();
                cohortFiles.addAll(getCohortFiles(analysisName, expression, "target"));
                cohortFiles.addAll(getCohortFiles(analysisName, expression, "outcome"));
                files.addAll(cohortFiles);
                List<String> cohortFileNames = cohortFiles.stream().map(MultipartFile::getName).collect(Collectors.toList());
                files.add(getRunner(analysis, cohortFileNames));
            } catch (IOException e) {
                logger.error(IR_BUILD_ERROR, e);
                throw new RuntimeIOException(IR_BUILD_ERROR, e);
            }
            return files;
        }).orElse(null);
    }

    private List<MultipartFile> getCohortFiles(String analysisName, Map<String, Object> analysisInfo,
                                               String property) {

        Objects.requireNonNull(analysisInfo, "Analysis should not be NULL");
        List<MultipartFile> files = new LinkedList<>();
        Object value = analysisInfo.get(property + ID_PROPERTY_SUFFIX);
        if (value instanceof List){
            for(Integer cohortId : ((List<Integer>)value)){
                String name = String.format("%s_%d_%s.sql", analysisName, cohortId, property);
                files.add(getCohortFile(cohortId, name, new String[]{"target_cohort_id"},
                        new String[]{ cohortId.toString() }));
            }
        }
        return files;
    }

    private MultipartFile getRunner(Map<String, Object> analysis, List<String> cohortFileNames) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("analysisId", analysis.get("id"));
        params.put("cohortDefinitions", cohortFileNames.stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(",")));
        String result = incidenceRatesTemplate.apply(params);
        return new MockMultipartFile("main.r", result.getBytes());
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.INCIDENCE;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[response.size()]));
    }

    @Override
    protected MultipartFile getAnalysisDescription(Map<String, Object> info) throws IOException {

        String result = ((String) info.getOrDefault("expression", "{}"))
                .replaceAll("\\\\", "");
        return new MockMultipartFile("analysisDescription.json", result.getBytes());
    }
}
