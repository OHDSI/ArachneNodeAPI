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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: September 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.achilles;

import static com.odysseusinc.arachne.datanode.Constants.CDM.CONCEPT_ID;
import static com.odysseusinc.arachne.datanode.Constants.CDM.concept_id;
import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.plainResultSet;
import static com.odysseusinc.arachne.datanode.util.datasource.QueryProcessors.statement;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.util.DataSourceUtils;
import com.odysseusinc.arachne.datanode.util.SqlUtils;
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
public class ProcedureReport extends BaseReport {

    public static final String PROCEDURE_PREVALENCE_BY_GENDER_AGE_YEAR_SQL = "classpath:/achilles/data/export/procedure/sqlPrevalenceByGenderAgeYear.sql";
    public static final String PROCEDURE_PREVALENCE_BY_MONTH_SQL = "classpath:/achilles/data/export/procedure/sqlPrevalenceByMonth.sql";
    public static final String PROCEDURE_FREQUENCY_DISTRIBUTION_SQL = "classpath:/achilles/data/export/procedure/sqlFrequencyDistribution.sql";
    public static final String PROCEDURE_PROCEDURES_BY_TYPE_SQL = "classpath:/achilles/data/export/procedure/sqlProceduresByType.sql";
    public static final String PROCEDURE_AGE_AT_FIRST_OCCURRENCE_SQL = "classpath:/achilles/data/export/procedure/sqlAgeAtFirstOccurrence.sql";

    @Autowired
    public ProcedureReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException {

        String prevalenceByGenderQuery = sqlUtils.transformSqlTemplate(dataSource, PROCEDURE_PREVALENCE_BY_GENDER_AGE_YEAR_SQL);
        String prevalenceByMonthQuery = sqlUtils.transformSqlTemplate(dataSource, PROCEDURE_PREVALENCE_BY_MONTH_SQL);
        String procedureFrequencyQuery = sqlUtils.transformSqlTemplate(dataSource, PROCEDURE_FREQUENCY_DISTRIBUTION_SQL);
        String proceduresByTypeQuery = sqlUtils.transformSqlTemplate(dataSource, PROCEDURE_PROCEDURES_BY_TYPE_SQL);
        String ageQuery = sqlUtils.transformSqlTemplate(dataSource, PROCEDURE_AGE_AT_FIRST_OCCURRENCE_SQL);

        return DataSourceUtils.<Map<Integer, String>>withDataSource(dataSource)
                .run(statement(prevalenceByGenderQuery))
                .forMapResults(concepts, CONCEPT_ID, "PREVALENCE_BY_GENDER_AGE_YEAR",
                        plainResultSet(concept_id, "trellis_name", "series_name", "x_calendar_year", "y_prevalence_1000pp"))
                .run(statement(prevalenceByMonthQuery))
                .forMapResults(concepts, CONCEPT_ID, "PREVALENCE_BY_MONTH",
                        plainResultSet(concept_id, "x_calendar_month", "y_prevalence_1000pp"))
                .run(statement(procedureFrequencyQuery))
                .forMapResults(concepts, CONCEPT_ID, "PROCEDURE_FREQUENCY_DISTRIBUTION",
                        plainResultSet(concept_id, "y_num_persons", "x_count"))
                .run(statement(proceduresByTypeQuery))
                .forMapResults(concepts, "PROCEDURE_CONCEPT_ID", "PROCEDURES_BY_TYPE",
                        plainResultSet("procedure_concept_id", "concept_name", "count_value"))
                .run(statement(ageQuery))
                .forMapResults(concepts, CONCEPT_ID, "AGE_AT_FIRST_OCCURRENCE",
                        plainResultSet(concept_id, "category", "min_value", "p10_value",
                                "p25_value", "median_value", "p75_value", "p90_value", "max_value"))
                .transform(ResultTransformers.toJsonMap(concepts))
                .write(ResultWriters.toMultipleFiles(targetDir, "procedure_%d.json", concepts))
                .getResultsCount();
    }
}
