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
 * Created: May 18, 2017
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.NO_ERROR;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.dto.achilles.AchillesJobDTO;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJob;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.repository.AchillesJobRepository;
import com.odysseusinc.arachne.datanode.repository.DataSourceRepository;
import com.odysseusinc.arachne.datanode.service.AchillesService;
import io.swagger.annotations.ApiOperation;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/achilles")
public class AchillesController {

    private static final String DATASOURCE_NOT_FOUND_EXCEPTION = "Datasource was not found";
    private static final String ACHILLES_JOB_NOT_FOUND_EXCEPTION = "Achilles job was not found";
    private final AchillesService achillesService;
    private final DataSourceRepository dataSourceRepository;
    private final AchillesJobRepository achillesJobRepository;
    private final GenericConversionService conversionService;

    @Autowired
    public AchillesController(AchillesService achillesService,
                              DataSourceRepository dataSourceRepository,
                              AchillesJobRepository achillesJobRepository,
                              GenericConversionService conversionService) {

        this.achillesService = achillesService;
        this.dataSourceRepository = dataSourceRepository;
        this.achillesJobRepository = achillesJobRepository;
        this.conversionService = conversionService;
    }

    @ApiOperation("Start Achilles for datasource")
    @RequestMapping(value = "{datasourceId}/jobs", method = POST)
    public JsonResult start(@PathVariable("datasourceId") Long datasourceId) throws NotExistException {

        DataSource dataSource = checkDataSource(datasourceId);
        achillesService.executeAchilles(dataSource);
        return new JsonResult(NO_ERROR);
    }

    @ApiOperation("Pull Achilles data from result tables")
    @RequestMapping(value = "{datasourceId}/pull", method = POST)
    public JsonResult pull(@PathVariable("datasourceId") Long datasourceId) throws NotExistException {

        DataSource dataSource = checkDataSource(datasourceId);
        AchillesJob job = achillesService.createAchillesImportJob(dataSource);
        if (job != null) {
            achillesService.pullAchillesData(job);
        }
        return new JsonResult(NO_ERROR);
    }

    @ApiOperation("Checks achilles_result availability")
    @RequestMapping(value = "{datasourceId}/pull", method = GET)
    public JsonResult<Boolean> hasAchillesResults(@PathVariable("datasourceId") Long datasourceId) throws NotExistException {

        DataSource dataSource = checkDataSource(datasourceId);
        Boolean result = achillesService.hasAchillesResultTable(dataSource);
        return new JsonResult<>(NO_ERROR, result);
    }

    @ApiOperation("Get latest job status for given datasource")
    @RequestMapping(value = "{datasourceId}/status", method = GET)
    public JsonResult<AchillesJobDTO> status(@PathVariable("datasourceId") Long datasourceId) throws NotExistException {

        DataSource dataSource = checkDataSource(datasourceId);
        Optional<AchillesJob> achillesJob = achillesJobRepository.findTopByDataSourceOrderByStarted(dataSource);
        AchillesJobDTO dto = conversionService.convert(achillesJob.orElse(defaultJob(dataSource)), AchillesJobDTO.class);
        return new JsonResult<>(NO_ERROR, dto);
    }

    @ApiOperation("Get Achilles jobs history for given datasource")
    @RequestMapping(value = "{datasourceId}/jobs", method = GET)
    public JsonResult<Page<AchillesJobDTO>> history(
            @PathVariable("datasourceId") Long datasourceId,
            @PageableDefault(sort = "started", direction = DESC) Pageable pageable
    ) throws NotExistException {

        DataSource dataSource = checkDataSource(datasourceId);
        Page<AchillesJobDTO> result = achillesJobRepository.findByDataSource(dataSource, pageable)
                .map(job -> conversionService.convert(job, AchillesJobDTO.class));
        return new JsonResult<>(NO_ERROR, result);
    }

    @ApiOperation("Get Achilles job log for given datasource and timestamp")
    @RequestMapping(value = "{datasourceId}/log/{started}", method = GET)
    public JsonResult<String> log(
            @PathVariable("datasourceId") Long datasourceId,
            @PathVariable("started") Long started
    ) throws NotExistException {

        DataSource dataSource = checkDataSource(datasourceId);
        AchillesJob job = achillesJobRepository.findByDataSourceAndStarted(dataSource, new Timestamp(started))
                .orElseThrow(() -> new NotExistException(ACHILLES_JOB_NOT_FOUND_EXCEPTION, AchillesJob.class));

        return new JsonResult<>(NO_ERROR, job.getAchillesLog());
    }

    private DataSource checkDataSource(Long datasourceId) throws NotExistException {

        return dataSourceRepository.findById(datasourceId)
                .orElseThrow(() -> new NotExistException(DATASOURCE_NOT_FOUND_EXCEPTION, DataSource.class));
    }

    private AchillesJob defaultJob(DataSource dataSource) {

        AchillesJob job = new AchillesJob();
        job.setDataSource(dataSource);
        job.setStatus(AchillesJobStatus.NOT_STARTED);
        return job;
    }
}
