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
 * Created: June 08, 2017
 *
 */

package com.odysseusinc.arachne.datanode.model.achilles;

import com.google.common.base.Objects;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "achilles_jobs")
public class AchillesJob {
    @Id
    @SequenceGenerator(name = "achilles_jobs_pk_sequence", sequenceName = "achilles_jobs_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "achilles_jobs_pk_sequence")
    private Long id;
    @NotNull
    @ManyToOne
    @JoinColumn(name = "datasource_id")
    private DataSource dataSource;
    @Column(name = "started", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date started;
    @Column(name = "finished")
    @Temporal(TemporalType.TIMESTAMP)
    private Date finished;
    @Column(name = "achilles_log")
    private String achillesLog;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AchillesJobStatus status;
    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    private AchillesJobSource source;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public DataSource getDataSource() {

        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {

        this.dataSource = dataSource;
    }

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

    public String getAchillesLog() {

        return achillesLog;
    }

    public void setAchillesLog(String achillesLog) {

        this.achillesLog = achillesLog;
    }

    public AchillesJobStatus getStatus() {

        return status;
    }

    public void setStatus(AchillesJobStatus status) {

        this.status = status;
    }

    public AchillesJobSource getSource() {

        return source;
    }

    public void setSource(AchillesJobSource source) {

        this.source = source;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AchillesJob that = (AchillesJob) o;
        return Objects.equal(id, that.id)
                && Objects.equal(dataSource, that.dataSource)
                && Objects.equal(started, that.started)
                && source == that.source
                && status == that.status;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(id, dataSource, started, status, source);
    }
}
