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
 * Created: November 01, 2016
 *
 */

package com.odysseusinc.arachne.datanode.dto.datanode;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.model.datanode.DataNodeStatus;
import java.util.Collection;

public class DataNodeDTO {
    private String id;
    private String name;
    private String description;

    private CommonUserDTO owner;
    private DataNodeStatus status;

    private Collection<DataSourceDTO> dataSources;

    private Boolean registered;

    private Boolean enabled;

    public String getId() {

        return id;
    }

    public void setId(String id) {

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

    public CommonUserDTO getOwner() {

        return owner;
    }

    public void setOwner(CommonUserDTO owner) {

        this.owner = owner;
    }

    public DataNodeStatus getStatus() {

        return status;
    }

    public void setStatus(DataNodeStatus status) {

        this.status = status;
    }

    public Collection<DataSourceDTO> getDataSources() {

        return dataSources;
    }

    public void setDataSources(Collection<DataSourceDTO> dataSources) {

        this.dataSources = dataSources;
    }

    public Boolean getRegistered() {

        return registered;
    }

    public void setRegistered(Boolean registered) {

        this.registered = registered;
    }

    public Boolean getEnabled() {

        return enabled;
    }

    public void setEnabled(Boolean enabled) {

        this.enabled = enabled;
    }
}
