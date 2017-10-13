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

import com.google.common.base.MoreObjects;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.util.SqlUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseReport implements ReportRunner {

    protected final Logger LOGGER = LoggerFactory.getLogger(ReportRunner.class);
    protected final SqlUtils sqlUtils;

    public BaseReport(SqlUtils sqlUtils) {

        this.sqlUtils = sqlUtils;
    }

    @Override
    public Integer runReports(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException {
        LOGGER.info("Starting report: {}", this);
        try {
            return execReport(dataSource, targetDir, concepts);
        }catch (IOException | SQLException e){
            LOGGER.error("Report failed: {}", this, e);
            throw e;
        } finally {
            LOGGER.info("Finished report: {}", this);
        }
    }

    protected abstract Integer execReport(DataSource dataSource, Path targetDir, List<Integer> concepts) throws IOException, SQLException;

    @Override
    public String toString() {

        return MoreObjects.toStringHelper(this)
                .toString();
    }
}
