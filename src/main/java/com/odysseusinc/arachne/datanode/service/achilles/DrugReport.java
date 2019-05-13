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
import static com.odysseusinc.arachne.datanode.Constants.CDM.DRUG_CONCEPT_ID;
import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.plainResultSet;
import static com.odysseusinc.arachne.datanode.util.datasource.QueryProcessors.statement;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.util.DataSourceUtils;
import com.odysseusinc.arachne.datanode.util.SqlUtils;
import com.odysseusinc.arachne.datanode.util.datasource.ResultSetProcessor;
import com.odysseusinc.arachne.datanode.util.datasource.ResultTransformers;
import com.odysseusinc.arachne.datanode.util.datasource.ResultWriters;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DrugReport extends BaseReport {

    public static final String DRUG_AGE_AT_FIRST_EXPOSURE_SQL = "classpath:/achilles/data/export/drug/sqlAgeAtFirstExposure.sql";
    public static final String DRUG_DAYS_SUPPLY_DISTRIBUTION_SQL = "classpath:/achilles/data/export/drug/sqlDaysSupplyDistribution.sql";
    public static final String DRUG_DRUGS_BY_TYPE_SQL = "classpath:/achilles/data/export/drug/sqlDrugsByType.sql";
    public static final String DRUG_PREVALENCE_BY_GENDER_AGE_YEAR_SQL = "classpath:/achilles/data/export/drug/sqlPrevalenceByGenderAgeYear.sql";
    public static final String DRUG_PREVALENCE_BY_MONTH_SQL = "classpath:/achilles/data/export/drug/sqlPrevalenceByMonth.sql";
    public static final String DRUG_FREQUENCY_DISTRIBUTION_SQL = "classpath:/achilles/data/export/drug/sqlFrequencyDistribution.sql";
    public static final String DRUG_QUANTITY_DISTRIBUTION_SQL = "classpath:/achilles/data/export/drug/sqlQuantityDistribution.sql";
    public static final String DRUG_REFILLS_DISTRIBUTION_SQL = "classpath:/achilles/data/export/drug/sqlRefillsDistribution.sql";

    public DrugReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException {

        String ageQuery = sqlUtils.transformSqlTemplate(dataSource, DRUG_AGE_AT_FIRST_EXPOSURE_SQL);
        String daysSupplyQuery = sqlUtils.transformSqlTemplate(dataSource, DRUG_DAYS_SUPPLY_DISTRIBUTION_SQL);
        String drugsByTypeQuery = sqlUtils.transformSqlTemplate(dataSource, DRUG_DRUGS_BY_TYPE_SQL);
        String prevalenceByGenderQuery = sqlUtils.transformSqlTemplate(dataSource, DRUG_PREVALENCE_BY_GENDER_AGE_YEAR_SQL);
        String prevalenceByMonthQuery = sqlUtils.transformSqlTemplate(dataSource, DRUG_PREVALENCE_BY_MONTH_SQL);
        String frequencyQuery = sqlUtils.transformSqlTemplate(dataSource, DRUG_FREQUENCY_DISTRIBUTION_SQL);
        String quantityQuery = sqlUtils.transformSqlTemplate(dataSource, DRUG_QUANTITY_DISTRIBUTION_SQL);
        String refillsQuery = sqlUtils.transformSqlTemplate(dataSource, DRUG_REFILLS_DISTRIBUTION_SQL);
        final ResultSetProcessor<Map> plainResultSet = plainResultSet("drug_concept_id",
                "category", "min_value", "p10_value", "p25_value", "median_value", "p75_value",
                "p90_value", "max_value");
        return DataSourceUtils.<Map<Integer, String>>withDataSource(dataSource)
                .run(statement(ageQuery))
                .forMapResults(concepts, DRUG_CONCEPT_ID, "AGE_AT_FIRST_EXPOSURE",
                        plainResultSet)
                .run(statement(daysSupplyQuery))
                .forMapResults(concepts, DRUG_CONCEPT_ID, "DAYS_SUPPLY_DISTRIBUTION",
                        plainResultSet)
                .run(statement(drugsByTypeQuery))
                .forMapResults(concepts, DRUG_CONCEPT_ID, "DRUGS_BY_TYPE",
                        plainResultSet("drug_concept_id", "concept_name", "count_value"))
                .run(statement(prevalenceByGenderQuery))
                .forMapResults(concepts, CONCEPT_ID, "PREVALENCE_BY_GENDER_AGE_YEAR",
                        plainResultSet(concept_id,
                                "trellis_name", "series_name", "x_calendar_year", "y_prevalence_1000pp"))
                .run(statement(prevalenceByMonthQuery))
                .forMapResults(concepts, CONCEPT_ID, "PREVALENCE_BY_MONTH",
                        plainResultSet(concept_id, "x_calendar_month", "y_prevalence_1000pp"))
                .run(statement(frequencyQuery))
                .forMapResults(concepts, CONCEPT_ID, "DRUG_FREQUENCY_DISTRIBUTION",
                        plainResultSet(concept_id, "y_num_persons", "x_count"))
                .run(statement(quantityQuery))
                .forMapResults(concepts, DRUG_CONCEPT_ID, "QUANTITY_DISTRIBUTION",
                        plainResultSet)
                .run(statement(refillsQuery))
                .forMapResults(concepts, DRUG_CONCEPT_ID, "REFILLS_DISTRIBUTION",
                        plainResultSet)
                .transform(ResultTransformers.toJsonMap(concepts))
                .write(ResultWriters.toMultipleFiles(targetDir, "drug_%d.json", concepts))
                .getResultsCount();
    }
}
