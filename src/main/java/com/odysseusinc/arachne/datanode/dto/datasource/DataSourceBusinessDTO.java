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
 * Created: January 13, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.datasource;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonCDMVersionDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonModelType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotBlank;

public class DataSourceBusinessDTO {

    private Long id;

    @Pattern(
            message = "Must be valid UUID.",
            regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    )
    @Deprecated
    private String uuid;

    @NotBlank
    private String name;
    @NotNull
    private CommonModelType modelType;
    @NotBlank
    private String organization;
    private CommonCDMVersionDTO cdmVersion;
    private String targetSchema;
    private String resultSchema;
    private String cohortTargetTable;
    private Boolean isRegistered;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getUuid() {

        return uuid;
    }

    public void setUuid(String uuid) {

        this.uuid = uuid;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public CommonModelType getModelType() {

        return modelType;
    }

    public void setModelType(CommonModelType modelType) {

        this.modelType = modelType;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public CommonCDMVersionDTO getCdmVersion() {
        return cdmVersion;
    }

    public void setCdmVersion(CommonCDMVersionDTO cdmVersion) {
        this.cdmVersion = cdmVersion;
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

    public Boolean getIsRegistered() {

        return isRegistered;
    }

    public void setIsRegistered(Boolean registered) {

        isRegistered = registered;
    }
}
