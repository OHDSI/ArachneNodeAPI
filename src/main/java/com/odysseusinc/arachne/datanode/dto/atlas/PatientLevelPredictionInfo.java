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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Nov 1, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.atlas;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

public class PatientLevelPredictionInfo extends BaseAtlasEntity implements Serializable {
    private Integer analysisId;
    private String modelType;
    private String createdBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd, HH:mm")
    private Date createdDate;
    private String modifiedBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd, HH:mm")
    private Date modifiedDate;

    public PatientLevelPredictionInfo() {

    }

    public Integer getAnalysisId() {

        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {

        this.analysisId = analysisId;
    }

    public String getModelType() {

        return modelType;
    }

    public void setModelType(String modelType) {

        this.modelType = modelType;
    }

    public String getCreatedBy() {

        return createdBy;
    }

    public void setCreatedBy(String createdBy) {

        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {

        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {

        this.createdDate = createdDate;
    }

    public String getModifiedBy() {

        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {

        this.modifiedBy = modifiedBy;
    }

    public Date getModifiedDate() {

        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {

        this.modifiedDate = modifiedDate;
    }
}

