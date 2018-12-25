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

import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.resultSet;
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
import org.springframework.stereotype.Component;

@Component
public class DeathReport extends BaseReport {

    public static final String DEATH_AGE_AT_FIRST_EXPOSURE_SQL = "classpath:/achilles/data/export_v5/death/sqlAgeAtDeath.sql";
    public static final String DEATH_DRUGS_BY_TYPE_SQL = "classpath:/achilles/data/export_v5/death/sqlDeathByType.sql";
    public static final String DEATH_PREVALENCE_BY_GENDER_AGE_YEAR_SQL = "classpath:/achilles/data/export_v5/death/sqlPrevalenceByGenderAgeYear.sql";
    public static final String DEATH_PREVALENCE_BY_MONTH_SQL = "classpath:/achilles/data/export_v5/death/sqlPrevalenceByMonth.sql";

    public DeathReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetPath, List<Integer> concepts) throws IOException, SQLException {

        String ageQuery = sqlUtils.transformSqlTemplate(dataSource, DEATH_AGE_AT_FIRST_EXPOSURE_SQL);
        String deathByTypeQuery = sqlUtils.transformSqlTemplate(dataSource, DEATH_DRUGS_BY_TYPE_SQL);
        String prevalenceByGenderQuery = sqlUtils.transformSqlTemplate(dataSource, DEATH_PREVALENCE_BY_GENDER_AGE_YEAR_SQL);
        String prevalenceByMonthQuery = sqlUtils.transformSqlTemplate(dataSource, DEATH_PREVALENCE_BY_MONTH_SQL);
        return DataSourceUtils.<String>withDataSource(dataSource)
                .run(statement(ageQuery))
                .mapResults("AGE_AT_DEATH", resultSet())
                .run(statement(deathByTypeQuery))
                .mapResults("DEATH_BY_TYPE", resultSet())
                .run(statement(prevalenceByGenderQuery))
                .mapResults("PREVALENCE_BY_GENDER_AGE_YEAR", resultSet())
                .run(statement(prevalenceByMonthQuery))
                .mapResults("PREVALENCE_BY_MONTH", resultSet())
                .transform(ResultTransformers.toJson())
                .write(ResultWriters.toFile(targetPath))
                .getResultsCount();
    }
}
