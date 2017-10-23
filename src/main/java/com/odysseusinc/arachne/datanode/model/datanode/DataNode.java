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
 * Created: November 01, 2016
 *
 */

package com.odysseusinc.arachne.datanode.model.datanode;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonHealthStatus;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.util.Set;

@Entity
@Table(name = "datanode")
public class DataNode {

    @Id
    @SequenceGenerator(name = "datanode_id_seq_generator", sequenceName = "datanode_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "datanode_id_seq_generator")
    private Long id;

    @Size(max = 50)
    @Column(length = 50, name = "sid")
    private String sid;

    @NotEmpty
    @Column(name = "name")
    private String name;

    @NotEmpty
    @Column(name = "description")
    private String description;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "dataNode")
    private Set<DataSource> dataSources;

    @Column(name = "health_status")
    @Enumerated(value = EnumType.STRING)
    private CommonHealthStatus healthStatus = CommonHealthStatus.NOT_COLLECTED;

    @Column(name = "health_status_description")
    private String healthStatusDescription;

    @NotEmpty
    @Column(name = "token")
    private String token;

    @Column(name = "central_id")
    private Long centralId;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    @Deprecated
    public String getSid() {

        return sid;
    }

    @Deprecated
    public void setSid(String sid) {

        this.sid = sid;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public Set<DataSource> getDataSources() {

        return dataSources;
    }

    public void setDataSources(Set<DataSource> dataSources) {

        this.dataSources = dataSources;
    }

    public CommonHealthStatus getHealthStatus() {

        return healthStatus;
    }

    public void setHealthStatus(CommonHealthStatus healthStatus) {

        this.healthStatus = healthStatus;
    }

    public String getHealthStatusDescription() {

        return healthStatusDescription;
    }

    public void setHealthStatusDescription(String healthStatusDescription) {

        this.healthStatusDescription = healthStatusDescription;
    }

    public String getToken() {

        return token;
    }

    public void setToken(String token) {

        this.token = token;
    }

    public Long getCentralId() {

        return centralId;
    }

    public void setCentralId(Long centralId) {

        this.centralId = centralId;
    }
}
