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

import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.ageAtFirstResultSet;
import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.resultSet;
import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.statsDataResultSet;
import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.statsResultSet;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ObservationPeriodReport extends BaseReport {

    public static final String OBSERVATIONPERIOD_AGEATFIRST_SQL = "classpath:/achilles/data/export_v5/observationperiod/ageatfirst.sql";
    public static final String OBSERVATIONPERIOD_AGEBYGENDER_SQL = "classpath:/achilles/data/export_v5/observationperiod/agebygender.sql";
    public static final String OBSERVATIONPERIOD_OBSERVATIONLENGTH_STATS_SQL = "classpath:/achilles/data/export_v5/observationperiod/observationlength_stats.sql";
    public static final String OBSERVATIONPERIOD_OBSERVATIONLENGTH_DATA_SQL = "classpath:/achilles/data/export_v5/observationperiod/observationlength_data.sql";
    public static final String OBSERVATIONPERIOD_CUMULATIVEDURATION_SQL = "classpath:/achilles/data/export_v5/observationperiod/cumulativeduration.sql";
    public static final String OBSERVATIONPERIOD_OBSERVATIONLENGTHBYGENDER_SQL = "classpath:/achilles/data/export_v5/observationperiod/observationlengthbygender.sql";
    public static final String OBSERVATIONPERIOD_OBSERVATIONLENGTHBYAGE_SQL = "classpath:/achilles/data/export_v5/observationperiod/observationlengthbyage.sql";
    public static final String OBSERVATIONPERIOD_OBSERVEDBYYEAR_STATS_SQL = "classpath:/achilles/data/export_v5/observationperiod/observedbyyear_stats.sql";
    public static final String OBSERVATIONPERIOD_OBSERVEDBYYEAR_DATA_SQL = "classpath:/achilles/data/export_v5/observationperiod/observedbyyear_data.sql";
    public static final String OBSERVATIONPERIOD_OBSERVEDBYMONTH_SQL = "classpath:/achilles/data/export_v5/observationperiod/observedbymonth.sql";
    public static final String OBSERVATIONPERIOD_PERIODSPERPERSON_SQL = "classpath:/achilles/data/export_v5/observationperiod/periodsperperson.sql";

    @Autowired
    public ObservationPeriodReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts)
            throws IOException, SQLException {

        String ageFirstQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_AGEATFIRST_SQL);
        String ageByGenerQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_AGEBYGENDER_SQL);
        String lenStatsQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_OBSERVATIONLENGTH_STATS_SQL);
        String lenDataQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_OBSERVATIONLENGTH_DATA_SQL);
        String durationQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_CUMULATIVEDURATION_SQL);
        String lenByGenerQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_OBSERVATIONLENGTHBYGENDER_SQL);
        String lenByAgeQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_OBSERVATIONLENGTHBYAGE_SQL);
        String byYearStatsQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_OBSERVEDBYYEAR_STATS_SQL);
        String byYearDataQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_OBSERVEDBYYEAR_DATA_SQL);
        String byMonthQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_OBSERVEDBYMONTH_SQL);
        String periodQuery = sqlUtils.transformSqlTemplate(dataSource, OBSERVATIONPERIOD_PERIODSPERPERSON_SQL);

        return DataSourceUtils.<String>withDataSource(dataSource)
                .run(statement(ageFirstQuery))
                .mapResults("AGE_AT_FIRST_OBSERVATION_HISTOGRAM", ageAtFirstResultSet())
                .run(statement(ageByGenerQuery))
                .mapResults("AGE_BY_GENDER", resultSet())
                .run(statement(lenStatsQuery))
                .mapResults("OBSERVATION_LENGTH_HISTOGRAM", statsResultSet())
                .run(statement(lenDataQuery))
                .mapResults("OBSERVATION_LENGTH_HISTOGRAM", statsDataResultSet())
                .run(statement(durationQuery))
                .mapResults("CUMULATIVE_DURATION", resultSet())
                .run(statement(lenByGenerQuery))
                .mapResults("OBSERVATION_PERIOD_LENGTH_BY_GENDER", resultSet())
                .run(statement(lenByAgeQuery))
                .mapResults("OBSERVATION_PERIOD_LENGTH_BY_AGE", resultSet())
                .run(statement(byYearStatsQuery))
                .mapResults("OBSERVED_BY_YEAR_HISTOGRAM", statsResultSet())
                .run(statement(byYearDataQuery))
                .mapResults("OBSERVED_BY_YEAR_HISTOGRAM", statsDataResultSet())
                .run(statement(byMonthQuery))
                .mapResults("OBSERVED_BY_MONTH", resultSet())
                .run(statement(periodQuery))
                .mapResults("PERSON_PERIODS_DATA", resultSet())
                .transform(ResultTransformers.toJson())
                .write(ResultWriters.toFile(targetDir))
                .getResultsCount();
    }


}
