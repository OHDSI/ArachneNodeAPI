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
 * Created: March 16, 2017
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import com.odysseusinc.arachne.datanode.BuildNumberDTO;
import com.odysseusinc.arachne.datanode.util.CentralUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BuildNumberController {

    @Value("${build.number}")
    private String buildNumber;
    @Value("${build.id}")
    private String buildId;
    @Value("${project.version}")
    private String projectVersion;

    private CentralUtil centralUtil;

    @Autowired
    public BuildNumberController(CentralUtil centralUtil) {

        this.centralUtil = centralUtil;
    }

    @ApiOperation(value = "Get build number")
    @RequestMapping(value = "/api/v1/build-number", method = RequestMethod.GET)
    public BuildNumberDTO buildNumber() {

        BuildNumberDTO buildNumberDTO = new BuildNumberDTO();
        buildNumberDTO.setBuildNumber(getBuildNumber());
        buildNumberDTO.setBuildId(getBuildId());
        buildNumberDTO.setProjectVersion(getProjectVersion());
        buildNumberDTO.setCentralUrl(centralUtil.getCentralUrl());
        return buildNumberDTO;
    }

    protected String getBuildNumber() {

        return buildNumber;
    }

    protected String getBuildId() {

        return buildId;
    }

    protected String getProjectVersion() {

        return projectVersion;
    }
}
