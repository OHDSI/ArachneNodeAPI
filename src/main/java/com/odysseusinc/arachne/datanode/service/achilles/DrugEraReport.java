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
 * Created: September 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.achilles;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.util.DataSourceUtils;
import com.odysseusinc.arachne.datanode.util.SqlUtils;
import com.odysseusinc.arachne.datanode.util.datasource.QueryProcessors;
import com.odysseusinc.arachne.datanode.util.datasource.ResultTransformers;
import com.odysseusinc.arachne.datanode.util.datasource.ResultWriters;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DrugEraReport extends BaseReport {
    public static final String DRUGERA_AGE_AT_FIRST_EXPOSURE_SQL = "classpath:/achilles/data/export_v5/drugera/sqlAgeAtFirstExposure.sql";
    public static final String DRUGERA_PREVALENCE_BY_GENDER_AGE_YEAR_SQL = "classpath:/achilles/data/export_v5/drugera/sqlPrevalenceByGenderAgeYear.sql";
    public static final String DRUGERA_PREVALENCE_BY_MONTH_SQL = "classpath:/achilles/data/export_v5/drugera/sqlPrevalenceByMonth.sql";
    public static final String DRUGERA_LENGTH_OF_ERA_SQL = "classpath:/achilles/data/export_v5/drugera/sqlLengthOfEra.sql";

    @Autowired
    public DrugEraReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException {

        String ageExposureQuery = sqlUtils.transformSqlTemplate(dataSource, DRUGERA_AGE_AT_FIRST_EXPOSURE_SQL);
        String prevalenceByGenderQuery = sqlUtils.transformSqlTemplate(dataSource, DRUGERA_PREVALENCE_BY_GENDER_AGE_YEAR_SQL);
        String prevalenceByMonth = sqlUtils.transformSqlTemplate(dataSource, DRUGERA_PREVALENCE_BY_MONTH_SQL);
        String lengthQuery = sqlUtils.transformSqlTemplate(dataSource, DRUGERA_LENGTH_OF_ERA_SQL);
        return DataSourceUtils.<Map<Integer, String>>withDataSource(dataSource)
                .run(QueryProcessors.statement(ageExposureQuery))
                .forMapResults(concepts, "CONCEPT_ID", "AGE_AT_FIRST_EXPOSURE",
                        AchillesProcessors.plainResultSet("concept_id", "category", "min_value", "p10_value", "p25_value", "median_value", "p75_value", "p90_value", "max_value"))
                .run(QueryProcessors.statement(prevalenceByGenderQuery))
                .forMapResults(concepts, "CONCEPT_ID", "PREVALENCE_BY_GENDER_AGE_YEAR",
                        AchillesProcessors.plainResultSet("concept_id", "trellis_name", "series_name", "x_calendar_year", "y_prevalence_1000pp"))
                .run(QueryProcessors.statement(prevalenceByMonth))
                .forMapResults(concepts, "CONCEPT_ID", "PREVALENCE_BY_MONTH",
                        AchillesProcessors.plainResultSet("concept_id", "x_calendar_month", "y_prevalence_1000pp"))
                .run(QueryProcessors.statement(lengthQuery))
                .forMapResults(concepts, "CONCEPT_ID", "LENGTH_OF_ERA",
                        AchillesProcessors.plainResultSet("concept_id", "category", "min_value", "p10_value", "p25_value", "median_value", "p75_value", "p90_value", "max_value"))
                .transform(ResultTransformers.toJsonMap(concepts))
                .write(ResultWriters.toMultipleFiles(targetDir, "drug_%d.json", concepts))
                .getResultsCount();
    }
}
