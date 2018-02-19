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
 * Created: September 11, 2017
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.NO_ERROR;
import static com.odysseusinc.arachne.datanode.util.DataSourceUtils.isNotDummyPassword;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonModelType;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.OptionDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.CreateDataSourceDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceBusinessDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import com.odysseusinc.arachne.datanode.service.UserService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DBMSType;
import io.swagger.annotations.ApiOperation;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class BaseDataSourceController<DS extends DataSource, BusinessDTO extends DataSourceBusinessDTO, CommonDTO extends CommonDataSourceDTO> extends BaseController {

    protected final DataSourceService dataSourceService;
    protected final BaseCentralIntegrationService<DS, CommonDTO> integrationService;
    protected final ModelMapper modelMapper;
    protected final GenericConversionService conversionService;
    protected final JmsTemplate jmsTemplate;
    protected final DestinationResolver destinationResolver;
    protected final CentralClient centralClient;
    protected final DataNodeService dataNodeService;

    protected BaseDataSourceController(UserService userService,
                                       ModelMapper modelMapper,
                                       BaseCentralIntegrationService<DS, CommonDTO> integrationService,
                                       DataSourceService dataSourceService,
                                       GenericConversionService conversionService,
                                       CentralClient centralClient,
                                       JmsTemplate jmsTemplate,
                                       DataNodeService dataNodeService) {

        super(userService);
        this.modelMapper = modelMapper;
        this.destinationResolver = jmsTemplate.getDestinationResolver();
        this.integrationService = integrationService;
        this.dataSourceService = dataSourceService;
        this.conversionService = conversionService;
        this.centralClient = centralClient;
        this.jmsTemplate = jmsTemplate;
        this.dataNodeService = dataNodeService;
    }

    @ApiOperation(value = "Add data source")
    @RequestMapping(value = Constants.Api.DataSource.ADD, method = RequestMethod.POST)
    public JsonResult<DataSourceDTO> add(Principal principal,
                                         @Valid @RequestBody CreateDataSourceDTO dataSourceDTO,
                                         BindingResult bindingResult
    ) throws NotExistException, PermissionDeniedException {

        if (bindingResult.hasErrors()) {
            return setValidationErrors(bindingResult);
        }
        final User user = getAdmin(principal);
        DataNode currentDataNode = dataNodeService.findCurrentDataNodeOrCreate(user);

        DataSource dataSource = conversionService.convert(dataSourceDTO, DataSource.class);
        CommonModelType type = checkDataSource(dataSource);

        dataSource.setDataNode(currentDataNode);
        CommonDTO commonDataSourceDTO = conversionService.convert(dataSource, getCommonDataSourceDTOClass());

        commonDataSourceDTO.setModelType(type);
        CommonDTO centralDTO = integrationService.sendDataSourceCreationRequest(
                user,
                dataSource.getDataNode(),
                commonDataSourceDTO
        ).getResult();
        dataSource.setCentralId(centralDTO.getId());

        DataSource optional = dataSourceService.create(user, dataSource, currentDataNode).get();
        JsonResult<DataSourceDTO> result = new JsonResult<>(NO_ERROR);
        result.setResult(masqueradePassword(modelMapper.map(optional, DataSourceDTO.class)));
        return result;
    }

    protected CommonModelType checkDataSource(DataSource dataSource) {

        return CommonModelType.CDM;
    }

    @ApiOperation(value = "Returns all data sources for current data node")
    @RequestMapping(
            value = Constants.Api.DataSource.ALL,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonResult<List<DataSourceDTO>> list(
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortAsc", required = false) Boolean sortAsc,
            Principal principal
    ) throws PermissionDeniedException {

        if (principal == null) {
            throw new AuthException("user not found");
        }
        JsonResult<List<DataSourceDTO>> result = new JsonResult<>(NO_ERROR);
        List<DataSourceDTO> dtos = dataSourceService.findAll(sortBy, sortAsc).stream()
                .map(dataSource -> modelMapper.map(dataSource, DataSourceDTO.class))
                .peek(this::masqueradePassword)
                .collect(Collectors.toList());

       JsonResult<List<CommonDataSourceDTO>> centralCommonDTOs = integrationService.getDataSources(getUser(principal),
               dtos.stream().map(DataSourceDTO::getCentralId).collect(Collectors.toList()));

        Map<Long, Boolean> idToPublished = centralCommonDTOs.getResult()
                .stream()
                .collect(Collectors.toMap(CommonDataSourceDTO::getId, CommonDataSourceDTO::getPublished));

        dtos.forEach(dto -> dto.setPublished(idToPublished.get(dto.getCentralId())));
        result.setResult(dtos);
        return result;
    }

    private DataSourceDTO masqueradePassword(DataSourceDTO dataSource){

        dataSource.setDbPassword(StringUtils.isEmpty(dataSource.getDbPassword()) ? "" : Constants.DUMMY_PASSWORD);
        return dataSource;
    }

    @ApiOperation(value = "Get data source")
    @RequestMapping(
            value = Constants.Api.DataSource.GET,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonResult<DataSourceDTO> get(Principal principal, @PathVariable("id") Long id) {

        if (principal == null) {
            throw new AuthException("user not found");
        }
        JsonResult<DataSourceDTO> result = new JsonResult<>(NO_ERROR);
        DataSource dataSource = dataSourceService.getById(id);
        result.setResult(masqueradePassword(modelMapper.map(dataSource, DataSourceDTO.class)));
        return result;
    }

    @ApiOperation(value = "Removes data source from current data node")
    @RequestMapping(
            value = Constants.Api.DataSource.DELETE,
            method = RequestMethod.DELETE
    )
    public JsonResult<Boolean> delete(Principal principal, @PathVariable("id") Long id) {

        if (principal == null) {
            throw new AuthException("user not found");
        }
        JsonResult centralResult = unpublishAndDeleteOnCentral(id);
        JsonResult<Boolean> result = new JsonResult<>();
        result.setErrorCode(centralResult.getErrorCode());

        if (NO_ERROR.getCode().equals(centralResult.getErrorCode())) {
            dataSourceService.delete(id);
            result.setResult(Boolean.TRUE);
        } else {
            result.setResult(Boolean.FALSE);
        }
        return result;
    }

    @ApiOperation(value = "Updates given data source")
    @RequestMapping(
            value = Constants.Api.DataSource.UPDATE,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonResult<DataSourceDTO> update(Principal principal,
                                            @Valid @RequestBody CreateDataSourceDTO dataSourceDTO,
                                            @PathVariable("id") Long id,
                                            BindingResult bindingResult)
            throws PermissionDeniedException {

        if (bindingResult.hasErrors()) {
            return setValidationErrors(bindingResult);
        }
        final User user = getAdmin(principal);
        DataSource dataSource = conversionService.convert(dataSourceDTO, DataSource.class);
        dataSource.setId(id);

        String inputPassword = dataSourceDTO.getDbPassword();
        dataSource.setPassword(isNotDummyPassword(inputPassword)? inputPassword: dataSourceService.getById(id).getPassword());

        CommonModelType type = checkDataSource(dataSource);
        final DataSource savedDataSource = dataSourceService.update(user, dataSource);

        CommonDTO commonDataSourceDTO = conversionService.convert(savedDataSource, getCommonDataSourceDTOClass());
        commonDataSourceDTO.setModelType(type);
        final JsonResult<CommonDTO> res = integrationService.sendDataSourceUpdateRequest(
                user,
                savedDataSource,
                commonDataSourceDTO
        );

        JsonResult<DataSourceDTO> result = new JsonResult<>();
        result.setErrorCode(res.getErrorCode());
        if (NO_ERROR.getCode().equals(res.getErrorCode())) {
            result.setResult(masqueradePassword(modelMapper.map(savedDataSource, DataSourceDTO.class)));
        } else {
            result.setValidatorErrors(res.getValidatorErrors());
            result.setErrorMessage(res.getErrorMessage());
        }
        return result;
    }

    public JsonResult unpublishAndDeleteOnCentral(Long dataSourceId) {

        DataSource dataSource = dataSourceService.getById(dataSourceId);
        return centralClient.unpublishAndSoftDeleteDataSource(dataSource.getCentralId());
    }

    @RequestMapping(
            value = "/api/v1/data-sources/dbms-types",
            method = RequestMethod.GET
    )
    public List<OptionDTO> getDBMSTypes() {

        return Arrays.stream(DBMSType.values())
                .map(dbms -> conversionService.convert(dbms, OptionDTO.class))
                .collect(Collectors.toList());
    }

    protected abstract Class<CommonDTO> getCommonDataSourceDTOClass();

}
