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
 * Created: December 22, 2016
 *
 */

package com.odysseusinc.arachne.datanode.dto.datasource;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;

public class CreateDataSourceDTO {

    private Long id;

    @NotBlank
    private String name;
    private String description;
    @NotNull
    private String dbmsType;
    @NotNull
    private String connectionString;

    @NotBlank
    private String cdmSchema;

    @NotBlank
    private String dbUsername;

    private String dbPassword;

    private String targetSchema;
    private String resultSchema;
    private String cohortTargetTable;

    private Boolean registred;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
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

    public String getDbmsType() {

        return dbmsType;
    }

    public void setDbmsType(String dbmsType) {

        this.dbmsType = dbmsType;
    }

    public String getConnectionString() {

        return connectionString;
    }

    public void setConnectionString(String connectionString) {

        this.connectionString = connectionString;
    }

    public Boolean getRegistred() {

        return registred;
    }

    public void setRegistred(Boolean registred) {

        this.registred = registred;
    }

    public String getCdmSchema() {

        return cdmSchema;
    }

    public void setCdmSchema(String cdmSchema) {

        this.cdmSchema = cdmSchema;
    }

    public String getDbUsername() {

        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {

        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {

        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {

        this.dbPassword = dbPassword;
    }

    public Boolean getIsRegistred() {

        return registred;
    }

    public void setIsRegistred(Boolean registred) {

        this.registred = registred;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
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
}
