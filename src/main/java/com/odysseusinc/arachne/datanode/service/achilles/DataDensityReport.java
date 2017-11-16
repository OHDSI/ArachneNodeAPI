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
public class DataDensityReport extends BaseReport {

    public static final String DATADENSITY_TOTALRECORDS_SQL = "classpath:/achilles/data/export_v5/datadensity/totalrecords.sql";
    public static final String DATADENSITY_RECORDSPERPERSON_SQL = "classpath:/achilles/data/export_v5/datadensity/recordsperperson.sql";
    public static final String DATADENSITY_CONCEPTSPERPERSON_SQL = "classpath:/achilles/data/export_v5/datadensity/conceptsperperson.sql";

    public DataDensityReport(SqlUtils sqlUtils) {

        super(sqlUtils);
    }

    @Override
    protected Integer execReport(DataSource dataSource, Path targetPath, List<Integer> concepts) throws IOException, SQLException {

        String totalQuery = sqlUtils.transformSqlTemplate(dataSource, DATADENSITY_TOTALRECORDS_SQL);
        String recordsPerPersonQuery = sqlUtils.transformSqlTemplate(dataSource, DATADENSITY_RECORDSPERPERSON_SQL);
        String conceptPerPersonQuery = sqlUtils.transformSqlTemplate(dataSource, DATADENSITY_CONCEPTSPERPERSON_SQL);

        return DataSourceUtils.<String>withDataSource(dataSource)
                .run(statement(totalQuery))
                .mapResults("TOTAL_RECORDS", resultSet())
                .run(statement(recordsPerPersonQuery))
                .mapResults("RECORDS_PER_PERSON", resultSet())
                .run(statement(conceptPerPersonQuery))
                .mapResults("CONCEPTS_PER_PERSON", resultSet())
                .transform(toJson())
                .write(toFile(targetPath))
                .getResultsCount();
    }
}
