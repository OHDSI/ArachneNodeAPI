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
 * Created: June 08, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.achilles;

import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJobSource;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus;
import java.util.Date;

public class AchillesJobDTO {
    private Date started;
    private Date finished;
    private AchillesJobStatus status;
    private DataSourceDTO dataSource;
    private AchillesJobSource source;

    public Date getStarted() {

        return started;
    }

    public void setStarted(Date started) {

        this.started = started;
    }

    public Date getFinished() {

        return finished;
    }

    public void setFinished(Date finished) {

        this.finished = finished;
    }

    public AchillesJobStatus getStatus() {

        return status;
    }

    public void setStatus(AchillesJobStatus status) {

        this.status = status;
    }

    public DataSourceDTO getDataSource() {

        return dataSource;
    }

    public void setDataSource(DataSourceDTO dataSource) {

        this.dataSource = dataSource;
    }

    public AchillesJobSource getSource() {

        return source;
    }

    public void setSource(AchillesJobSource source) {

        this.source = source;
    }
}
