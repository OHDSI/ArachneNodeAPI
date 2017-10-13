/**
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
 * Created: September 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.achilles;

import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.plainResultSet;
import static com.odysseusinc.arachne.datanode.util.datasource.QueryProcessors.statement;
import static com.odysseusinc.arachne.datanode.util.datasource.ResultTransformers.toJsonMap;
import static com.odysseusinc.arachne.datanode.util.datasource.ResultWriters.toMultipleFiles;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.util.DataSourceUtils;
import com.odysseusinc.arachne.datanode.util.SqlUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VisitReport extends BaseReport {

    public static final String VISIT_PREVALENCE_BY_GENDER_AGE_YEAR_SQL = "classpath:/achilles/data/export_v5/visit/sqlPrevalenceByGenderAgeYear.sql";
    public static final String VISIT_PREVALENCE_BY_MONTH_SQL = "classpath:/achilles/data/export_v5/visit/sqlPrevalenceByMonth.sql";
    public static final String VISIT_DURATION_BY_TYPE_SQL = "classpath:/achilles/data/export_v5/visit/sqlVisitDurationByType.sql";
    public static final String VISIT_AGE_AT_FIRST_OCCURRENCE_SQL = "classpath:/achilles/data/export_v5/visit/sqlAgeAtFirstOccurrence.sql";

    @Autowired
    public VisitReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException {

        String prevalenceByGender = sqlUtils.transformSqlTemplate(dataSource, VISIT_PREVALENCE_BY_GENDER_AGE_YEAR_SQL);
        String prevalenceByMonth = sqlUtils.transformSqlTemplate(dataSource, VISIT_PREVALENCE_BY_MONTH_SQL);
        String visitDuration = sqlUtils.transformSqlTemplate(dataSource, VISIT_DURATION_BY_TYPE_SQL);
        String ageAtFirst = sqlUtils.transformSqlTemplate(dataSource, VISIT_AGE_AT_FIRST_OCCURRENCE_SQL);

        return DataSourceUtils.<Map<Integer, String>>withDataSource(dataSource)
                .run(statement(prevalenceByGender))
                .forMapResults(concepts, "CONCEPT_ID", "PREVALENCE_BY_GENDER_AGE_YEAR",
                        plainResultSet("concept_id", "trellis_name", "series_name", "x_calendar_year", "y_prevalence_1000pp"))
                .run(statement(prevalenceByMonth))
                .forMapResults(concepts, "CONCEPT_ID", "PREVALENCE_BY_MONTH",
                        plainResultSet("concept_id", "x_calendar_month", "y_prevalence_1000pp"))
                .run(statement(visitDuration))
                .forMapResults(concepts, "CONCEPT_ID", "VISIT_DURATION_BY_TYPE",
                        plainResultSet("concept_id"))
                .run(statement(ageAtFirst))
                .forMapResults(concepts, "CONCEPT_ID", "AGE_AT_FIRST_OCCURRENCE",
                        plainResultSet("concept_id"))
                .transform(toJsonMap(concepts))
                .write(toMultipleFiles(targetDir, "visit_%d.json", concepts))
                .getResultsCount();
    }
}
