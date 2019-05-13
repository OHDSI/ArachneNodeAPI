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
public class PersonReport extends BaseReport {

    public static final String PERSON_POPULATION_SQL = "classpath:/achilles/data/export/person/population.sql";
    public static final String PERSON_GENDER_SQL = "classpath:/achilles/data/export/person/gender.sql";
    public static final String PERSON_RACE_SQL = "classpath:/achilles/data/export/person/race.sql";
    public static final String PERSON_ETHNICITY_SQL = "classpath:/achilles/data/export/person/ethnicity.sql";
    public static final String PERSON_YEAROFBIRTH_STATS_SQL = "classpath:/achilles/data/export/person/yearofbirth_stats.sql";
    public static final String PERSON_YEAROFBIRTH_DATA_SQL = "classpath:/achilles/data/export/person/yearofbirth_data.sql";

    @Autowired
    public PersonReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetPath, List<Integer> concepts) throws IOException, SQLException {

        String summaryQuery = sqlUtils.transformSqlTemplate(dataSource, PERSON_POPULATION_SQL);
        String genderQuery = sqlUtils.transformSqlTemplate(dataSource, PERSON_GENDER_SQL);
        String raceQuery = sqlUtils.transformSqlTemplate(dataSource, PERSON_RACE_SQL);
        String ethinicityQuery = sqlUtils.transformSqlTemplate(dataSource, PERSON_ETHNICITY_SQL);
        String birthYearQuery = sqlUtils.transformSqlTemplate(dataSource, PERSON_YEAROFBIRTH_STATS_SQL);
        String birthDataQuery = sqlUtils.transformSqlTemplate(dataSource, PERSON_YEAROFBIRTH_DATA_SQL);

        return DataSourceUtils.<String>withDataSource(dataSource)
                .run(statement(summaryQuery))
                .mapResults("SUMMARY", resultSet())
                .run(statement(genderQuery))
                .mapResults("GENDER_DATA", resultSet())
                .run(statement(raceQuery))
                .mapResults("RACE_DATA", resultSet())
                .run(statement(ethinicityQuery))
                .mapResults("ETHNICITY_DATA", resultSet())
                .run(statement(birthYearQuery))
                .mapResults("BIRTH_YEAR_HISTOGRAM", statsResultSet())
                .run(statement(birthDataQuery))
                .mapResults("BIRTH_YEAR_HISTOGRAM", statsDataResultSet())
                .transform(ResultTransformers.toJson())
                .write(ResultWriters.toFile(targetPath))
                .getResultsCount();
    }

}
