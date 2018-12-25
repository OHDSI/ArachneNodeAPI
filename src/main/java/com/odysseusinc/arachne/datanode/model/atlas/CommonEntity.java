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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: July 27, 2017
 *
 */

package com.odysseusinc.arachne.datanode.model.atlas;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "common_entity")
public class CommonEntity {
    @Id
    @SequenceGenerator(name = "common_entity_id_seq_generator", sequenceName = "common_entity_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "common_entity_id_seq_generator")
    private Long id;

    @Column(name = "guid")
    @NotNull
    private String guid;

    @ManyToOne
    private Atlas origin;

    @Column(name = "analysis_type")
    @Enumerated(value = EnumType.STRING)
    @NotNull
    private CommonAnalysisType analysisType;

    @Column(name = "local_id")
    @NotNull
    private Integer localId;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getGuid() {

        return guid;
    }

    public void setGuid(String guid) {

        this.guid = guid;
    }

    public Atlas getOrigin() {

        return origin;
    }

    public void setOrigin(Atlas origin) {

        this.origin = origin;
    }

    public CommonAnalysisType getAnalysisType() {

        return analysisType;
    }

    public void setAnalysisType(CommonAnalysisType analysisType) {

        this.analysisType = analysisType;
    }

    public Integer getLocalId() {

        return localId;
    }

    public void setLocalId(Integer localId) {

        this.localId = localId;
    }
}
