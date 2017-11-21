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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonPredictionDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionAnalysisInfo;
import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionInfo;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PatientLevelPredictionRequestHandler implements AtlasRequestHandler<CommonPredictionDTO, List<MultipartFile>> {

    private static final Logger logger = LoggerFactory.getLogger(PatientLevelPredictionRequestHandler.class);
    public static final String INITIAL_SUFFIX = " initial population.sql";
    public static final String OUTCOME_SUFFIX = " outcome population.sql";
    public static final String PLP_FOLDER = "plp/";
    public static final String PACKRAT_RUN_R = "packratRun.r";
    public static final String PACKRAT_RUN_R_LOCATION = PLP_FOLDER + PACKRAT_RUN_R;
    public static final String PACKRAT_ARCHIVE = "PatientLevelPredictionAnalysis.tar.gz";

    private final AtlasClient atlasClient;
    private final GenericConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private final CentralSystemClient centralClient;
    private final SqlRenderService sqlRenderService;
    private final Template patientLevelPredictionRunnerTemplate;

    @Autowired
    public PatientLevelPredictionRequestHandler(AtlasClient atlasClient,
                                                GenericConversionService conversionService,
                                                CommonEntityService commonEntityService,
                                                CentralSystemClient centralClient,
                                                SqlRenderService sqlRenderService,
                                                @Qualifier("patientLevelPredictionRunnerTemplate")
                                                            Template patientLevelPredictionRunnerTemplate) {

        this.atlasClient = atlasClient;
        this.conversionService = conversionService;
        this.commonEntityService = commonEntityService;
        this.centralClient = centralClient;
        this.sqlRenderService = sqlRenderService;
        this.patientLevelPredictionRunnerTemplate = patientLevelPredictionRunnerTemplate;
    }

    @Override
    public List<CommonPredictionDTO> getObjectsList() {

        List<PatientLevelPredictionInfo> result = atlasClient.getPatientLevelPredictions();
        return result.stream()
                .map(plp -> conversionService.convert(plp, CommonPredictionDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> {
            PatientLevelPredictionAnalysisInfo info = atlasClient.getPatientLevelPrediction(entity.getLocalId());
            List<MultipartFile> files = new ArrayList<>(6);
            try {
                String initialName = info.getName() + INITIAL_SUFFIX;
                String outcomeName = info.getName() + OUTCOME_SUFFIX;

                files.add(getAnalysisDescription(info));
                files.add(getResourceFile(PACKRAT_RUN_R, PACKRAT_RUN_R_LOCATION));
                files.add(getCohortFile(info.getTreatmentId(), initialName));
                files.add(getCohortFile(info.getOutcomeId(), outcomeName));
                files.add(getRunner(initialName, outcomeName));
            }catch (IOException e){
                logger.error("Failed to build PLP data", e);
                throw new RuntimeIOException("Failed to build PLP data", e);
            }
            return files;
        }).orElse(null);
    }

    private MultipartFile getRunner(String initialName, String outcomeName) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("initialFileName", initialName);
        params.put("outcomeFileName", outcomeName);
        String result = patientLevelPredictionRunnerTemplate.apply(params);
        return new MockMultipartFile("main.r", result.getBytes());
    }

    private MultipartFile getCohortFile(Integer cohortId, String name) {
         CohortDefinition cohort = atlasClient.getCohortDefinition(cohortId);
         if (Objects.nonNull(cohort)) {
             String content = sqlRenderService.renderSql(cohort);
             if (Objects.nonNull(content)) {
                 return new MockMultipartFile(name, content.getBytes());
             }
         }
         return null;
    }

    private MultipartFile getAnalysisDescription(PatientLevelPredictionAnalysisInfo info) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        String result = mapper.writeValueAsString(info);
        return new MockMultipartFile("analysisDescription.json", result.getBytes());
    }

    private MultipartFile getResourceFile(String name, String path) throws IOException {

        Resource file = new ClassPathResource(path);
        try(InputStream in = file.getInputStream()) {
            return new MockMultipartFile(name, IOUtils.toByteArray(in));
        }
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.PREDICTION;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[response.size()]));
    }
}
