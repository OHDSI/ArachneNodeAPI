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
public class MeasurementReport extends BaseReport {

    public static final String PREVALENCE_BY_GENDER_AGE_YEAR_SQL = "classpath:/achilles/data/export_v5/measurement/sqlPrevalenceByGenderAgeYear.sql";
    public static final String PREVALENCE_BY_MONTH_SQL = "classpath:/achilles/data/export_v5/measurement/sqlPrevalenceByMonth.sql";
    public static final String FREQUENCY_DISTRIBUTION_SQL = "classpath:/achilles/data/export_v5/measurement/sqlFrequencyDistribution.sql";
    public static final String MEASUREMENTS_BY_TYPE_SQL = "classpath:/achilles/data/export_v5/measurement/sqlMeasurementsByType.sql";
    public static final String AGE_AT_FIRST_OCCURRENCE_SQL = "classpath:/achilles/data/export_v5/measurement/sqlAgeAtFirstOccurrence.sql";
    public static final String RECORDS_BY_UNIT_SQL = "classpath:/achilles/data/export_v5/measurement/sqlRecordsByUnit.sql";
    public static final String MEASUREMENT_VALUE_DISTRIBUTION_SQL = "classpath:/achilles/data/export_v5/measurement/sqlMeasurementValueDistribution.sql";
    public static final String LOWER_LIMIT_DISTRIBUTION_SQL = "classpath:/achilles/data/export_v5/measurement/sqlLowerLimitDistribution.sql";
    public static final String UPPER_LIMIT_DISTRIBUTION_SQL = "classpath:/achilles/data/export_v5/measurement/sqlUpperLimitDistribution.sql";
    public static final String VALUES_RELATIVE_TO_NORM_SQL = "classpath:/achilles/data/export_v5/measurement/sqlValuesRelativeToNorm.sql";

    @Autowired
    public MeasurementReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException {

        String prevalenceByGender = sqlUtils.transformSqlTemplate(dataSource, PREVALENCE_BY_GENDER_AGE_YEAR_SQL);
        String prevalenceByMonth = sqlUtils.transformSqlTemplate(dataSource, PREVALENCE_BY_MONTH_SQL);
        String frequency = sqlUtils.transformSqlTemplate(dataSource, FREQUENCY_DISTRIBUTION_SQL);
        String byType = sqlUtils.transformSqlTemplate(dataSource, MEASUREMENTS_BY_TYPE_SQL);
        String ageAtFirst = sqlUtils.transformSqlTemplate(dataSource, AGE_AT_FIRST_OCCURRENCE_SQL);
        String recordByUnit = sqlUtils.transformSqlTemplate(dataSource, RECORDS_BY_UNIT_SQL);
        String valueDist = sqlUtils.transformSqlTemplate(dataSource, MEASUREMENT_VALUE_DISTRIBUTION_SQL);
        String lowerLimit = sqlUtils.transformSqlTemplate(dataSource, LOWER_LIMIT_DISTRIBUTION_SQL);
        String upperLimit = sqlUtils.transformSqlTemplate(dataSource, UPPER_LIMIT_DISTRIBUTION_SQL);
        String valuesRelative = sqlUtils.transformSqlTemplate(dataSource, VALUES_RELATIVE_TO_NORM_SQL);

        return DataSourceUtils.<Map<Integer, String>>withDataSource(dataSource)
                .run(statement(prevalenceByGender))
                .forMapResults(concepts, "CONCEPT_ID", "PREVALENCE_BY_GENDER_AGE_YEAR",
                        plainResultSet("concept_id", "trellis_name", "series_name", "x_calendar_year", "y_prevalence_1000pp"))
                .run(statement(prevalenceByMonth))
                .forMapResults(concepts, "CONCEPT_ID", "PREVALENCE_BY_MONTH",
                        plainResultSet("concept_id", "x_calendar_month", "y_prevalence_1000pp"))
                .run(statement(frequency))
                .forMapResults(concepts, "CONCEPT_ID", "FREQUENCY_DISTRIBUTION",
                        plainResultSet("concept_id", "y_num_persons", "x_count"))
                .run(statement(byType))
                .forMapResults(concepts, "MEASUREMENT_CONCEPT_ID", "MEASUREMENTS_BY_TYPE",
                        plainResultSet("measurement_concept_id", "concept_name", "count_value"))
                .run(statement(ageAtFirst))
                .forMapResults(concepts, "CONCEPT_ID", "AGE_AT_FIRST_OCCURRENCE",
                        plainResultSet("concept_id"))
                .run(statement(recordByUnit))
                .forMapResults(concepts, "MEASUREMENT_CONCEPT_ID", "RECORDS_BY_UNIT",
                        plainResultSet("measurement_concept_id", "concept_name", "count_value"))
                .run(statement(valueDist))
                .forMapResults(concepts, "CONCEPT_ID", "MEASUREMENT_VALUE_DISTRIBUTION",
                        plainResultSet("concept_id"))
                .run(statement(lowerLimit))
                .forMapResults(concepts, "CONCEPT_ID", "LOWER_LIMIT_DISTRIBUTION",
                        plainResultSet("concept_id"))
                .run(statement(upperLimit))
                .forMapResults(concepts, "CONCEPT_ID", "UPPER_LIMIT_DISTRIBUTION",
                        plainResultSet("concept_id"))
                .run(statement(valuesRelative))
                .forMapResults(concepts, "MEASUREMENT_CONCEPT_ID", "VALUES_RELATIVE_TO_NORM",
                        plainResultSet("measurement_concept_id", "concept_name", "count_value"))
                .transform(toJsonMap(concepts))
                .write(toMultipleFiles(targetDir, "measurement_%d.json", concepts))
                .getResultsCount();
    }
}
