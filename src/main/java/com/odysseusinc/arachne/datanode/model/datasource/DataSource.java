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
 * Created: November 01, 2016
 *
 */

package com.odysseusinc.arachne.datanode.model.datasource;

import com.google.common.base.MoreObjects;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonHealthStatus;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;
import org.hibernate.validator.constraints.NotBlank;

@Entity
@Table(name = "datasource")
@SQLDelete(sql = "UPDATE datasource SET deleted_at = current_timestamp WHERE id = ?")
public class DataSource {

    @Id
    @SequenceGenerator(name = "datasource_id_seq_generator", sequenceName = "datasource_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "datasource_id_seq_generator")
    private Long id;

    @Column(name = "sid", nullable = false)
    private String uuid;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false)
    private DBMSType type;

    @NotNull
    @Column
    private String connectionString;

    @NotBlank
    @Column(nullable = false)
    private String cdmSchema;

    @NotBlank
    @Column(name = "dbms_username", nullable = false)
    private String username;


    @Column(name = "dbms_password", nullable = true)
    @Type(type = "encryptedString")
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    private DataNode dataNode;

    @Column
    @Enumerated(value = EnumType.STRING)
    private CommonHealthStatus healthStatus = CommonHealthStatus.NOT_COLLECTED;

    @Column
    private Date deletedAt;
    
    @Column
    private String healthStatusDescription;

    @Column
    private String targetSchema;

    @Column
    private String resultSchema;

    @Column
    private String cohortTargetTable;

    private Long centralId;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    @Deprecated
    public String getUuid() {

        return uuid;
    }

    @Deprecated
    public void setUuid(String uuid) {

        this.uuid = uuid;
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


    public DBMSType getType() {

        return type;
    }

    public void setType(DBMSType type) {

        this.type = type;
    }

    public String getConnectionString() {

        return connectionString;
    }

    public void setConnectionString(String connectionString) {

        this.connectionString = connectionString;
    }

    public String getCdmSchema() {

        return cdmSchema;
    }

    public void setCdmSchema(String cdmSchema) {

        this.cdmSchema = cdmSchema;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(String password) {

        this.password = password;
    }

    public DataNode getDataNode() {

        return dataNode;
    }

    public void setDataNode(DataNode dataNode) {

        this.dataNode = dataNode;
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

    public Long getCentralId() {

        return centralId;
    }

    public void setCentralId(Long centralId) {

        this.centralId = centralId;
    }

    public String getTargetSchema() {

        return targetSchema;
    }

    public void setTargetSchema(String atlasTargetDbSchema) {

        this.targetSchema = atlasTargetDbSchema;
    }

    public String getResultSchema() {

        return resultSchema;
    }

    public void setResultSchema(String resultSchema) {

        this.resultSchema = resultSchema;
    }

    public String getCohortTargetTable() {

        return cohortTargetTable;
    }

    public void setCohortTargetTable(String cohortTargetTable) {

        this.cohortTargetTable = cohortTargetTable;
    }

    @Override
    public String toString() {

        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("type", type)
                .add("connectionString", connectionString)
                .add("cdmSchema", cdmSchema)
                .add("username", "***")
                .add("password", "***")
                .add("dataNode", dataNode)
                .add("healthStatus", healthStatus)
                .add("healthStatusDescription", healthStatusDescription)
                .add("targetSchema", targetSchema)
                .add("resultSchema", resultSchema)
                .add("cohortTargetTable", cohortTargetTable)
                .add("centralId", centralId)
                .toString();
    }

    public Date getDeletedAt() {

        return deletedAt;
    }

    public void setDeletedAt(final Date deletedAt) {

        this.deletedAt = deletedAt;
    }
}
