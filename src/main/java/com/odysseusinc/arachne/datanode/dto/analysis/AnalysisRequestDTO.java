/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Jul 8, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.analysis;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;

public class AnalysisRequestDTO {

    @NotBlank
    private String executableFileName;

    @NotNull
    private Long datasourceId;

    @NotNull
    private String title;

    private String study;

    @NotNull
    private CommonAnalysisType type;

    public String getExecutableFileName() {

        return executableFileName;
    }

    public void setExecutableFileName(String executableFileName) {

        this.executableFileName = executableFileName;
    }

    public Long getDatasourceId() {

        return datasourceId;
    }

    public void setDatasourceId(Long datasourceId) {

        this.datasourceId = datasourceId;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle(String title) {

        this.title = title;
    }

    public String getStudy() {

        return study;
    }

    public void setStudy(String study) {

        this.study = study;
    }

    public CommonAnalysisType getType() {

        return type;
    }

    public void setType(CommonAnalysisType type) {

        this.type = type;
    }
}
