/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
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
 * Created: June 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonListEntityRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonListEntityResponseDTO;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.repository.AtlasRepository;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CohortService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class CohortServiceImpl implements CohortService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CohortServiceImpl.class);

    private static final String CHECKING_COHORT_LISTS_REQUESTS_LOG = "Checking Cohort Lists Requests";
    private static final String CHECKING_COHORT_REQUESTS_LOG = "Checking Cohort Requests";
    private static final String PROCESS_LIST_REQUEST_FAILURE_LOG = "Process List Requests checking failure, {}";
    private static final String PROCESS_REQUEST_FAILURE_LOG = "Process request checking failure, {}";
    private final CentralSystemClient centralClient;
    private final ConfigurableListableBeanFactory beanFactory;
    private final AtlasRepository atlasRepository;
    private Map<CommonAnalysisType,
            AtlasRequestHandler<? extends CommonEntityDTO, ? extends CommonEntityDTO>> handlerMap =
            new HashMap<>();

    public CohortServiceImpl(CentralSystemClient centralClient,
                             ConfigurableListableBeanFactory beanFactory,
                             AtlasRepository atlasRepository) {

        this.centralClient = centralClient;
        this.beanFactory = beanFactory;
        this.atlasRepository = atlasRepository;
    }

    @PostConstruct
    public void init() {

        Map<String, AtlasRequestHandler> beans = beanFactory.getBeansOfType(AtlasRequestHandler.class);

        putBeans(beans, (v) -> !beanFactory.getBeanDefinition(v.getKey()).isPrimary());
        putBeans(beans, (v) -> beanFactory.getBeanDefinition(v.getKey()).isPrimary());

    }

    private void putBeans(Map<String, AtlasRequestHandler> beans, Predicate<? super Map.Entry<String, AtlasRequestHandler>> predicate) {

        beans.entrySet()
                .stream()
                .filter(predicate)
                .forEach(entry -> handlerMap.put(entry.getValue().getAnalysisType(), entry.getValue()));
    }

    @Override
    public void checkListRequests() {

        LOGGER.debug(CHECKING_COHORT_LISTS_REQUESTS_LOG);
        try {
            final CommonListEntityRequest requests = centralClient.getEntityListRequests();
            if (CollectionUtils.isEmpty(requests.getRequestMap())) {
                return;
            }
            requests.getRequestMap().forEach((id, requestObject) -> {
                if (handlerMap.containsKey(requestObject.getEntityType())) {

                    LOGGER.info(
                            "Requesting entity list for type "
                                    + requestObject.getEntityType()
                                    + " for Atlas ids = "
                                    + requestObject.getAtlasIdList().stream().map(Object::toString).collect(Collectors.joining(", "))
                    );

                    List<Atlas> requestAtlasList = atlasRepository.findByCentralIdIn(requestObject.getAtlasIdList());
                    Map<Long, Long> atlasIdMap = requestAtlasList.stream().collect(Collectors.toMap(Atlas::getId, Atlas::getCentralId));

                    AtlasRequestHandler<? extends CommonEntityDTO, ? extends CommonEntityDTO> handler = handlerMap.get(requestObject.getEntityType());
                    List<? extends CommonEntityDTO> list = handler.getObjectsList(requestAtlasList);

                    list.forEach(entry -> entry.setOriginId(atlasIdMap.get(entry.getOriginId())));

                    CommonListEntityResponseDTO result =
                            new CommonListEntityResponseDTO(Sets.newHashSet(id), list);
                    centralClient.sendListEntityResponse(result);
                } else {
                    LOGGER.warn("Handler of type {} was not registered", requestObject.getEntityType());
                }
            });
        } catch (Exception ex) {
            LOGGER.error(PROCESS_LIST_REQUEST_FAILURE_LOG, ex.getMessage());
        }
    }

    @Override
    public boolean isPreprocessingIgnored(File file) {

        try {
            String firstLine = Files.asCharSource(file, StandardCharsets.UTF_8).readFirstLine();
            return firstLine != null && firstLine.startsWith(IGNORE_PREPROCESSING_MARK);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void checkCohortRequest() {

        LOGGER.debug(CHECKING_COHORT_REQUESTS_LOG);
        try {
            centralClient.getEntityRequests().forEach(request -> {
                if (handlerMap.containsKey(request.getEntityType())) {
                    AtlasRequestHandler handler = handlerMap.get(
                            request.getEntityType());
                    handler.sendResponse(
                            handler.getAtlasObject(request.getEntityGuid()),
                            request.getId()
                    );
                }
            });
        } catch (Exception ex) {
            LOGGER.error(PROCESS_REQUEST_FAILURE_LOG, ex);
            LOGGER.error(PROCESS_REQUEST_FAILURE_LOG, ex.getMessage());
        }
    }

    /**
     * @param sourceStatement MS SQL Statement
     * @param parameters      Atlas original parameters (NULL at Atlas'es controller)
     * @param dbmsType        DBMSType for target Native SQL
     * @param options         Parameters for placeholders replacement (e.g. CDM schema, Cohorts table, etc)
     * @return Native SQL in accordiance with dbmsType
     */
    @Override
    public String translateSQL(String sourceStatement, Map<String, String> parameters,
                               DBMSType dbmsType, TranslateOptions options) {

        String translated;
        try {
            String[] parameterKeys = getMapKeys(parameters);
            String[] parameterValues = getMapValues(parameters, parameterKeys);

            String renderedSQL = SqlRender.renderSql(sourceStatement, parameterKeys, parameterValues);

            if (dbmsType == null
                    || DBMSType.MS_SQL_SERVER == dbmsType
                    || DBMSType.PDW == dbmsType) {
                translated = renderedSQL;
            } else {
                translated = translateSql(dbmsType, renderedSQL);
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return processPlaceHolders(translated, options);
    }

    private synchronized String translateSql(DBMSType dbmsType, String renderedSQL) {

        return SqlTranslate.translateSql(
                renderedSQL,
                DBMSType.MS_SQL_SERVER.getOhdsiDB(),
                dbmsType.getOhdsiDB()
        );
    }

    private String processPlaceHolders(String expression, TranslateOptions options) {

        for (Map.Entry<String, String> replacement : options.replacements.entrySet()) {
            expression = expression.replaceAll(replacement.getKey(), replacement.getValue());
        }
        return expression;
    }

    private String[] getMapKeys(Map<String, String> parameters) {

        if (parameters == null) {
            return null;
        } else {
            return parameters.keySet().toArray(new String[parameters.keySet().size()]);
        }
    }

    private String[] getMapValues(Map<String, String> parameters, String[] parameterKeys) {

        ArrayList<String> parameterValues = new ArrayList<>();
        if (parameters == null) {
            return null;
        } else {
            for (String parameterKey : parameterKeys) {
                parameterValues.add(parameters.get(parameterKey));
            }
            return parameterValues.toArray(new String[parameterValues.size()]);
        }
    }

    public static class TranslateOptions {

        private final Map<String, String> replacements;

        public TranslateOptions(String cdmDataBaseSchema,
                                String targetDataBaseSchema,
                                String resultsDatabaseSchema,
                                String vocabDatabaseSchema,
                                String targetCohortTable,
                                int targetCohortId) {

            replacements = ImmutableMap.<String, String>builder()
                    .put("@cdm_database_schema", cdmDataBaseSchema)
                    .put("@results_database_schema", resultsDatabaseSchema)
                    .put("@target_database_schema", targetDataBaseSchema)
                    .put("@vocab_database_schema", vocabDatabaseSchema)
                    .put("@target_cohort_table", targetCohortTable)
                    .put("@target_cohort_id", String.valueOf(targetCohortId))
                    .put("@generateStats;", "1")
                    .build();
        }
    }
}
