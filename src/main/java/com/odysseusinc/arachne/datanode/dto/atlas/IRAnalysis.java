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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Nov 3, 2017
 *
 */

package com.odysseusinc.arachne.datanode.dto.atlas;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.odysseusinc.arachne.datanode.dto.serialize.UserNameDeserializer;
import com.odysseusinc.arachne.datanode.dto.serialize.MultiFormatDateDeserializer;
import java.util.Date;

public class IRAnalysis extends BaseAtlasEntity {
    private Integer id;
    private String description;
    @JsonDeserialize(using = UserNameDeserializer.class)
    private String createdBy;
    @JsonDeserialize(using = MultiFormatDateDeserializer.class)
    private Date createdDate;
    @JsonDeserialize(using = UserNameDeserializer.class)
    private String modifiedBy;
    @JsonDeserialize(using = MultiFormatDateDeserializer.class)
    private Date modifiedDate;

    public Integer getId() {

        return id;
    }

    public void setId(Integer id) {

        this.id = id;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
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
