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
 * Created: September 21, 2017
 *
 */

package com.odysseusinc.arachne.datanode.util;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.service.CohortService;
import com.odysseusinc.arachne.datanode.service.impl.CohortServiceImpl;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class SqlUtils {

    private final ApplicationContext applicationContext;
    private final CohortService cohortService;

    public SqlUtils(ApplicationContext applicationContext,
                    CohortService cohortService) {

        this.applicationContext = applicationContext;
        this.cohortService = cohortService;
    }

    public String transformSqlTemplate(DataSource dataSource, String name) throws IOException {

        String sql = readSql(name);
        final DBMSType target = dataSource.getType();
        final String cdmSchema = dataSource.getCdmSchema();
        final String targetSchema = dataSource.getTargetSchema();
        final String resultSchema = dataSource.getResultSchema();
        final String cohortTargetTable = dataSource.getCohortTargetTable();
        final CohortServiceImpl.TranslateOptions options = new CohortServiceImpl.TranslateOptions(
                cdmSchema,
                StringUtils.defaultIfEmpty(targetSchema, cdmSchema),
                StringUtils.defaultIfEmpty(resultSchema, cdmSchema),
                cdmSchema,
                StringUtils.defaultIfEmpty(cohortTargetTable, "cohort"),
                0
        );
        final String nativeStatement
                = cohortService.translateSQL(sql, null, target, options);
        return nativeStatement;
    }

    protected String readSql(String name) throws IOException {

        Resource resource = applicationContext.getResource(name);
        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            return IOUtils.toString(reader);
        }
    }
}
