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

import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.ageAtFirstResultSet;
import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.resultSet;
import static com.odysseusinc.arachne.datanode.util.datasource.QueryProcessors.statement;
import static com.odysseusinc.arachne.datanode.util.datasource.ResultTransformers.toJson;
import static com.odysseusinc.arachne.datanode.util.datasource.ResultWriters.toFile;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.util.DataSourceUtils;
import com.odysseusinc.arachne.datanode.util.SqlUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DashboardReport extends BaseReport {

    public DashboardReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException {

        String summaryQuery = sqlUtils.transformSqlTemplate(dataSource, PersonReport.PERSON_POPULATION_SQL);
        String genderQuery = sqlUtils.transformSqlTemplate(dataSource, PersonReport.PERSON_GENDER_SQL);
        String ageFirstQuery = sqlUtils.transformSqlTemplate(dataSource, ObservationPeriodReport.OBSERVATIONPERIOD_AGEATFIRST_SQL);
        String durationQuery = sqlUtils.transformSqlTemplate(dataSource, ObservationPeriodReport.OBSERVATIONPERIOD_CUMULATIVEDURATION_SQL);
        String byMonthQuery = sqlUtils.transformSqlTemplate(dataSource, ObservationPeriodReport.OBSERVATIONPERIOD_OBSERVEDBYMONTH_SQL);

        return DataSourceUtils.<String>withDataSource(dataSource)
                .run(statement(summaryQuery))
                .mapResults("SUMMARY", resultSet())
                .run(statement(genderQuery))
                .mapResults("GENDER_DATA", resultSet())
                .run(statement(ageFirstQuery))
                .mapResults("AGE_AT_FIRST_OBSERVATION_HISTOGRAM", ageAtFirstResultSet())
                .run(statement(durationQuery))
                .mapResults("CUMULATIVE_DURATION", resultSet())
                .run(statement(byMonthQuery))
                .mapResults("OBSERVED_BY_MONTH", resultSet())
                .transform(toJson())
                .write(toFile(targetDir))
                .getResultsCount();
    }
}
