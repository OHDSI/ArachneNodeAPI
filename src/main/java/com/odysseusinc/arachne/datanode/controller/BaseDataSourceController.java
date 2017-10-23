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

import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonModelType;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.OptionDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.CreateDataSourceDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import com.odysseusinc.arachne.datanode.service.UserService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.datanode.service.impl.DataSourceHelperImpl;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DBMSType;
import io.swagger.annotations.ApiOperation;
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

import javax.jms.JMSException;
import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseDataSourceController<DS extends DataSource, DTO extends CommonDataSourceDTO> extends BaseController{

    protected final DataSourceService dataSourceService;
    protected final BaseCentralIntegrationService<DS, DTO> integrationService;
    protected final ModelMapper modelMapper;
    protected final GenericConversionService conversionService;
    protected final JmsTemplate jmsTemplate;
    protected final DestinationResolver destinationResolver;
    protected final DataSourceHelperImpl dataSourceHelper;
    protected final CentralClient centralClient;

    protected BaseDataSourceController(UserService userService,
                                       ModelMapper modelMapper,
                                       BaseCentralIntegrationService<DS, DTO> integrationService,
                                       DataSourceService dataSourceService,
                                       GenericConversionService conversionService,
                                       DataSourceHelperImpl dataSourceHelper,
                                       CentralClient centralClient,
                                       JmsTemplate jmsTemplate) {

        super(userService);
        this.modelMapper = modelMapper;
        this.destinationResolver = jmsTemplate.getDestinationResolver();
        this.integrationService = integrationService;
        this.dataSourceService = dataSourceService;
        this.conversionService = conversionService;
        this.dataSourceHelper = dataSourceHelper;
        this.centralClient = centralClient;
        this.jmsTemplate = jmsTemplate;
    }

    @ApiOperation(value = "Add data source")
    @RequestMapping(value = Constants.Api.DataSource.ADD, method = RequestMethod.POST)
    public JsonResult<DataSourceDTO> add(Principal principal,
                                         @Valid @RequestBody CreateDataSourceDTO dataSourceDTO,
                                         BindingResult bindingResult
    ) throws NotExistException, PermissionDeniedException, IOException, JMSException {

        if (bindingResult.hasErrors()) {
            return setValidationErrors(bindingResult);
        }
        final User user = getAdmin(principal);
        final DataSource dataSource = conversionService.convert(dataSourceDTO, DataSource.class);
        final CommonModelType modelType = checkDataSource(dataSource);
        dataSource.setModelType(modelType);
        JsonResult<DataSourceDTO> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        dataSourceService.create(user, dataSource)
                .ifPresent(ds -> result.setResult(modelMapper.map(ds, DataSourceDTO.class)));
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
    public JsonResult<List<DataSourceDTO>> list(Principal principal) {

        if (principal == null) {
            throw new AuthException("user not found");
        }
        JsonResult<List<DataSourceDTO>> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        List<DataSourceDTO> dtos = dataSourceService.findAll().stream()
                .map(dataSource -> modelMapper.map(dataSource, DataSourceDTO.class))
                .collect(Collectors.toList());
        result.setResult(dtos);
        return result;
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
        JsonResult<DataSourceDTO> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        result.setResult(modelMapper.map(dataSourceService.getById(id), DataSourceDTO.class));
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
        dataSourceService.delete(id);
        JsonResult<Boolean> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        result.setResult(Boolean.TRUE);
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
            throws PermissionDeniedException, IOException, JMSException {

        if (bindingResult.hasErrors()) {
            return setValidationErrors(bindingResult);
        }
        final User user = getAdmin(principal);
        JsonResult<DataSourceDTO> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        DataSource dataSource = conversionService.convert(dataSourceDTO, DataSource.class);
        final CommonModelType modelType = checkDataSource(dataSource);
        dataSource.setModelType(modelType);
        dataSource.setId(id);
        dataSourceService.update(user, dataSource)
                .ifPresent(saved -> {
                    result.setResult(modelMapper.map(saved, DataSourceDTO.class));
                });
        return result;
    }

    @ApiOperation(value = "Register datasource on arachne central")
    @RequestMapping(
            value = Constants.Api.DataSource.CENTRAL_REGISTER,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonResult<CommonDataSourceDTO> register(
            Principal principal,
            @PathVariable("id") Long id,
            @RequestBody DTO commonDataSourceDTO
    ) throws SQLException, PermissionDeniedException {

        final User user = getAdmin(principal);
        DataSource dataSource = dataSourceService.getById(id);
        commonDataSourceDTO.setId(dataSource.getId());
        commonDataSourceDTO.setUuid(dataSource.getUuid());
        JsonResult<CommonDataSourceDTO> jsonResult = integrationService.sendDataSourceRegistrationRequest(
                user,
                dataSource.getDataNode(),
                commonDataSourceDTO
        );
        dataSource.setCentralId(jsonResult.getResult().getId());
        if (JsonResult.ErrorCode.NO_ERROR.getCode().equals(jsonResult.getErrorCode())) {
            dataSourceService.markDataSourceAsRegistered(dataSource.getCentralId());
        }
        return jsonResult;
    }

    @ApiOperation(value = "Unregister datasource on arachne central")
    @RequestMapping(
            value = Constants.Api.DataSource.CENTRAL_REGISTER,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonResult unregister(@PathVariable("id") Long id)
            throws SQLException, PermissionDeniedException {

        DataSource dataSource = dataSourceService.getById(id);
        final Long dataNodeCentralId = dataSource.getDataNode().getCentralId();
        final JsonResult result = centralClient.unregisterDataSource(dataNodeCentralId, dataSource.getCentralId());
        if (JsonResult.ErrorCode.NO_ERROR.getCode().equals(result.getErrorCode())) {
            dataSourceService.markDataSourceAsUnregistered(dataSource.getCentralId());
        }
        return result;
    }

    @ApiOperation(value = "Get business data of data source")
    @RequestMapping(
            value = Constants.Api.DataSource.GET_BUSINESS,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonResult<Map<String, Object>> getBusiness(
            Principal principal,
            @PathVariable("id") Long id
    ) throws AuthException, NotExistException {

        DataSource dataSource = dataSourceService.getById(id);
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            throw new AuthException("user not found");
        }

        Map<String, Object> commonDataSourceDTO;
        if (dataSource.getRegistred()) {
            commonDataSourceDTO = (Map<String, Object>) this.integrationService
                    .getDataSource(user, dataSource.getCentralId())
                    .getResult();
        } else {
            commonDataSourceDTO = new HashMap<>();
            commonDataSourceDTO.put("name", dataSource.getName());
        }

        // Set local data source id
        commonDataSourceDTO.put("id", dataSource.getId());
        commonDataSourceDTO.put("isRegistered", dataSource.getRegistred());
        commonDataSourceDTO.put("modelType", dataSource.getModelType());

        JsonResult<Map<String, Object>> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        result.setResult(commonDataSourceDTO);
        return result;
    }

    @ApiOperation(value = "Update business data of data source")
    @RequestMapping(
            value = Constants.Api.DataSource.UPDATE_BUSINESS,
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonResult<DTO> updateBusiness(
            Principal principal,
            @PathVariable("id") Long id,
            @RequestBody DTO commonDataSourceDTO
    ) throws AuthException, NotExistException, PermissionDeniedException {

        final User user = getAdmin(principal);
        DS dataSource = (DS) dataSourceService.getById(id);
        commonDataSourceDTO.setUuid(dataSource.getUuid());
        return this.integrationService.updateDataSource(user, dataSource, commonDataSourceDTO);
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
}
