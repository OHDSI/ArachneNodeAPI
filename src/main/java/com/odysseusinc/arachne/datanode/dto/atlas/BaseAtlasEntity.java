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
 * Authors: Pavel Grafkin
 * Created: March 15, 2018
 *
 */

package com.odysseusinc.arachne.datanode.dto.atlas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;

public class BaseAtlasEntity {

    @JsonIgnore
    protected Atlas origin;

    protected String name;

    public Atlas getOrigin() {

        return origin;
    }

    public void setOrigin(Atlas origin) {

        this.origin = origin;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }
}