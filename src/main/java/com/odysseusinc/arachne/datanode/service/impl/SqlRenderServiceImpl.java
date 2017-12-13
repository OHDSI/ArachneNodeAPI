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
 * Created: Nov 20, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import java.io.IOException;
import java.util.Objects;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.ohdsi.sql.SqlRender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SqlRenderServiceImpl implements SqlRenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlRenderService.class);
    private final CohortExpressionQueryBuilder queryBuilder;

    public SqlRenderServiceImpl(CohortExpressionQueryBuilder queryBuilder) {

        this.queryBuilder = queryBuilder;
    }

    @Override
    public String renderSql(CohortDefinition cohort) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            if (Objects.nonNull(cohort.getExpression())) {
                CohortExpression expression = mapper.readValue(cohort.getExpression(), CohortExpression.class);
                final CohortExpressionQueryBuilder.BuildExpressionQueryOptions options = new CohortExpressionQueryBuilder.BuildExpressionQueryOptions();
                String expressionSql = queryBuilder.buildExpressionQuery(expression, options);
                String[] parameters = new String[]{"target_cohort_id"};
                String[] values = new String[]{ cohort.getId().toString() };
                return SqlRender.renderSql(expressionSql, parameters, values);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to construct cohort", e);
        }
        return null;
    }
}
