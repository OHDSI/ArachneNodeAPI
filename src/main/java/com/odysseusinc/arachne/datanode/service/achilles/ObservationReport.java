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
public class ObservationReport extends BaseReport {

    public static final String OBSERVATION_PREVALENCE_BY_GENDER_AGE_YEAR_SQL = "classpath:/achilles/data/export/observation/sqlPrevalenceByGenderAgeYear.sql";
    public static final String OBSERVATION_PREVALENCE_BY_MONTH_SQL = "classpath:/achilles/data/export/observation/sqlPrevalenceByMonth.sql";
    public static final String OBSERVATION_FREQUENCY_DISTRIBUTION_SQL = "classpath:/achilles/data/export/observation/sqlFrequencyDistribution.sql";
    public static final String OBSERVATION_OBSERVATIONS_BY_TYPE_SQL = "classpath:/achilles/data/export/observation/sqlObservationsByType.sql";
    public static final String OBSERVATION_AGE_AT_FIRST_OCCURRENCE_SQL = "classpath:/achilles/data/export/observation/sqlAgeAtFirstOccurrence.sql";

    @Autowired
    public ObservationReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException {

        String prevalenceByGender = sqlUtils.transformSqlTemplate(dataSource, OBSERVATION_PREVALENCE_BY_GENDER_AGE_YEAR_SQL);
        String prevalenceByMonth = sqlUtils.transformSqlTemplate(dataSource, OBSERVATION_PREVALENCE_BY_MONTH_SQL);
        String frequency = sqlUtils.transformSqlTemplate(dataSource, OBSERVATION_FREQUENCY_DISTRIBUTION_SQL);
        String byType = sqlUtils.transformSqlTemplate(dataSource, OBSERVATION_OBSERVATIONS_BY_TYPE_SQL);
        String ageAtFirst = sqlUtils.transformSqlTemplate(dataSource, OBSERVATION_AGE_AT_FIRST_OCCURRENCE_SQL);

        return DataSourceUtils.<Map<Integer,String>>withDataSource(dataSource)
                .run(statement(prevalenceByGender))
                .forMapResults(concepts, CONCEPT_ID, "PREVALENCE_BY_GENDER_AGE_YEAR",
                        plainResultSet(concept_id, "trellis_name", "series_name", "x_calendar_year", "y_prevalence_1000pp"))
                .run(statement(prevalenceByMonth))
                .forMapResults(concepts, CONCEPT_ID, "PREVALENCE_BY_MONTH",
                        plainResultSet(concept_id, "x_calendar_month", "y_prevalence_1000pp"))
                .run(statement(frequency))
                .forMapResults(concepts, CONCEPT_ID, "OBS_FREQUENCY_DISTRIBUTION",
                        plainResultSet(concept_id, "y_num_persons", "x_count"))
                .run(statement(byType))
                .forMapResults(concepts, CONCEPT_ID, "OBSERVATIONS_BY_TYPE",
                        plainResultSet(concept_id, "concept_name", "count_value"))
                .run(statement(ageAtFirst))
                .forMapResults(concepts, CONCEPT_ID, "AGE_AT_FIRST_OCCURRENCE",
                        plainResultSet(concept_id, "category", "min_value", "p10_value",
                                "p25_value", "median_value", "p75_value", "p90_value", "max_value"))
                .transform(toJsonMap(concepts))
                .write(toMultipleFiles(targetDir, "observation_%d.json", concepts))
                .getResultsCount();
    }
}
