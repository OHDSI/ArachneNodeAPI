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
 * Created: Nov 3, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;

import static com.odysseusinc.arachne.commons.utils.CommonFileUtils.ANALYSIS_INFO_FILE_DESCRIPTION;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonIncidenceRatesDTO;
import com.odysseusinc.arachne.commons.utils.AnalysisArchiveUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.IRAnalysis;
import com.odysseusinc.arachne.datanode.exception.ArachneSystemRuntimeException;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class IncidenceRatesRequestHandler extends BaseRequestHandler implements AtlasRequestHandler<CommonIncidenceRatesDTO, List<MultipartFile>> {

    private static final Logger logger = LoggerFactory.getLogger(IncidenceRatesRequestHandler.class);
    public static final String IR_BUILD_ERROR = "Failed to build IR data";
    public static final String ID_PROPERTY_SUFFIX = "Ids";
    private static final String PACKAGE_NAME = "IncidenceRate%d";
    private static final String SKELETON_RESOURCE = "/ir/hydra/IncidenceRate_v0.0.1.zip";
    private final GenericConversionService conversionService;
    private final CentralSystemClient centralClient;
    private final CommonEntityService commonEntityService;
    private Template incidenceRatesTemplate;

    @Autowired
    public IncidenceRatesRequestHandler(AtlasService atlasService,
                                        GenericConversionService conversionService,
                                        CentralSystemClient centralClient,
                                        CommonEntityService commonEntityService,
                                        SqlRenderService sqlRenderService,
                                        @Qualifier("incidenceRatesRunnerTemplate")
                                        Template incidenceRatesTemplate) {

        super(sqlRenderService, atlasService);
        this.conversionService = conversionService;
        this.centralClient = centralClient;
        this.commonEntityService = commonEntityService;
        this.incidenceRatesTemplate = incidenceRatesTemplate;
    }

    @Override
    public List<CommonIncidenceRatesDTO> getObjectsList(List<Atlas> atlasList) {

        List<IRAnalysis> irAnalyses = atlasService.execute(atlasList, AtlasClient::getIncidenceRates);
        return irAnalyses.stream()
                .map(ir -> conversionService.convert(ir, CommonIncidenceRatesDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> {
            Integer localId = entity.getLocalId();
            String packageName = String.format(PACKAGE_NAME, localId);
            Map<String, Object> analysis = atlasService.execute(entity.getOrigin(), atlasClient -> atlasClient.getIncidenceRate(localId));
            String analysisName = (String) analysis.getOrDefault("name", "ir_analysis");
            List<MultipartFile> files = new ArrayList<>();
            try {
                ObjectMapper mapper = new ObjectMapper();
                String expressionValue = (String) analysis.getOrDefault("expression", "");
                Map<String, Object> expression = mapper.readValue(expressionValue, Map.class);
                List<MultipartFile> cohortFiles = new LinkedList<>();
                cohortFiles.addAll(getCohortFiles(entity.getOrigin(), analysisName, expression, "target"));
                cohortFiles.addAll(getCohortFiles(entity.getOrigin(), analysisName, expression, "outcome"));
                files.addAll(cohortFiles);
                List<String> cohortFileNames = cohortFiles.stream().map(MultipartFile::getName).collect(Collectors.toList());

                JsonNode json = mapper.valueToTree(expression);
                byte[] content = atlasService.hydrateAnalysis(json, packageName, SKELETON_RESOURCE);
                String filename = AnalysisArchiveUtils.getArchiveFileName(getAnalysisType(), AnalysisArchiveUtils.getAnalysisName(analysis));

                MultipartFile file = new MockMultipartFile(filename, filename, MediaType.APPLICATION_OCTET_STREAM_VALUE, content);
                files.add(file);
                files.add(getRunner(analysis, cohortFileNames, packageName, filename, String.format("analysis_%d", localId)));
                MultipartFile descriptionFile = new MockMultipartFile("file", ANALYSIS_INFO_FILE_DESCRIPTION, MediaType.TEXT_PLAIN_VALUE, analysisName.getBytes());
                files.add(descriptionFile);
            } catch (IOException e) {
                logger.error(IR_BUILD_ERROR, e);
                throw new ArachneSystemRuntimeException(IR_BUILD_ERROR, e);
            }
            return files;
        }).orElse(null);
    }

    private List<MultipartFile> getCohortFiles(Atlas origin, String analysisName, Map<String, Object> analysisInfo,
                                               String property) {

        Objects.requireNonNull(analysisInfo, "Analysis should not be NULL");
        List<MultipartFile> files = new LinkedList<>();
        Object value = analysisInfo.get(property + ID_PROPERTY_SUFFIX);
        if (value instanceof List){
            for(Integer cohortId : ((List<Integer>)value)){
                String name = String.format("%s_%d_%s.sql", analysisName, cohortId, property);
                files.add(getCohortFile(origin, cohortId, name, new String[]{"target_cohort_id"},
                        new String[]{ cohortId.toString() }));
            }
        }
        return files;
    }

    private MultipartFile getRunner(Map<String, Object> analysis, List<String> cohortFileNames, String packageName, String packageFile, String analysisDir) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("analysisId", analysis.get("id"));
        params.put("cohortDefinitions", cohortFileNames.stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(",")));
        params.put("packageName", packageName);
        params.put("analysisDir", analysisDir);
        params.put("packageFile", packageFile);
        String result = incidenceRatesTemplate.apply(params);
        return new MockMultipartFile("file", "main.r", MediaType.TEXT_PLAIN_VALUE, result.getBytes());
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.INCIDENCE;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[response.size()]));
    }

}
