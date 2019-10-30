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
 * Created: September 11, 2017
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.NO_ERROR;
import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.VALIDATION_ERROR;
import static java.util.Arrays.asList;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.OptionDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.commons.utils.ConverterUtils;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.datasource.CreateDataSourceDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceBusinessDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.BadRequestException;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.exception.ServiceNotAvailableException;
import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import com.odysseusinc.arachne.datanode.service.UserService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.datanode.util.DataNodeUtils;
import com.odysseusinc.arachne.datanode.util.LogUtils;
import feign.FeignException;
import feign.RetryableException;
import io.swagger.annotations.ApiOperation;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.ohdsi.authenticator.exception.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

public abstract class BaseDataSourceController<DS extends DataSource, BusinessDTO extends DataSourceBusinessDTO, CommonDTO extends CommonDataSourceDTO> extends BaseController {

    private static final String COMMUNICATION_FAILED = "Failed to communicate with Central, error: {}";
    private static final String AUTH_ERROR_MESSAGE = "Couldn't autheticate on Central, {}";
    private static final String UNEXPECTED_ERROR = "Unexpected error during request to Central, {}";
    protected final DataSourceService dataSourceService;
    protected final BaseCentralIntegrationService<DS, CommonDTO> integrationService;
    protected final ModelMapper modelMapper;
    protected final GenericConversionService conversionService;
    protected final JmsTemplate jmsTemplate;
    protected final DestinationResolver destinationResolver;
    protected final CentralClient centralClient;
    protected final DataNodeService dataNodeService;
    protected final ConverterUtils converterUtils;
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDataSourceController.class);

    protected BaseDataSourceController(UserService userService,
                                       ModelMapper modelMapper,
                                       BaseCentralIntegrationService<DS, CommonDTO> integrationService,
                                       DataSourceService dataSourceService,
                                       GenericConversionService conversionService,
                                       CentralClient centralClient,
                                       JmsTemplate jmsTemplate,
                                       DataNodeService dataNodeService,
                                       ConverterUtils converterUtils) {

        super(userService);
        this.modelMapper = modelMapper;
        this.destinationResolver = jmsTemplate.getDestinationResolver();
        this.integrationService = integrationService;
        this.dataSourceService = dataSourceService;
        this.conversionService = conversionService;
        this.centralClient = centralClient;
        this.jmsTemplate = jmsTemplate;
        this.dataNodeService = dataNodeService;
        this.converterUtils = converterUtils;
    }

    @ApiOperation(value = "Add data source")
    @RequestMapping(value = Constants.Api.DataSource.ADD, method = RequestMethod.POST)
    public JsonResult<DataSourceDTO> add(Principal principal,
                                         @Valid @RequestPart("dataSource") CreateDataSourceDTO dataSourceDTO,
                                         @RequestPart(name = "keyfile", required = false) MultipartFile keyfile,
                                         BindingResult bindingResult
    ) throws NotExistException, PermissionDeniedException {

        if (bindingResult.hasErrors()) {
            return setValidationErrors(bindingResult);
        }
        final User user = getAdmin(principal);
        dataSourceDTO.setKeyfile(keyfile);
        DataSource dataSource = conversionService.convert(dataSourceDTO, DataSource.class);
        DataSource optional = dataSourceService.create(user, dataSource);
        JsonResult<DataSourceDTO> result = new JsonResult<>(NO_ERROR);
        result.setResult(conversionService.convert(optional, DataSourceDTO.class));
        return result;
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
        List<DataSourceDTO> dtos = dataSourceService.findAllNotDeleted(sortBy, sortAsc).stream()
                .map(dataSource -> conversionService.convert(dataSource, DataSourceDTO.class))
                .collect(Collectors.toList());
        FunctionalMode mode = dataNodeService.getDataNodeMode();

        if (!CollectionUtils.isEmpty(dtos) && Objects.equals(mode, FunctionalMode.NETWORK)) {
            dtos = setFieldsFromCentral(getUser(principal), dtos);
        }
        result.setResult(dtos);
        return result;
    }

    private List<DataSourceDTO> setFieldsFromCentral(User user, List<DataSourceDTO> dtos) {

        try {
            JsonResult<List<CommonDataSourceDTO>> centralCommonDTOs =
                    integrationService.getDataSources(user,
                            dtos.stream()
                                    .map(DataSourceDTO::getCentralId)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()));

            Map<Long, CommonDataSourceDTO> idToDto = centralCommonDTOs.getResult()
                    .stream()
                    .collect(Collectors.toMap(CommonDataSourceDTO::getId, e -> e));

            dtos.forEach(e -> {
                CommonDataSourceDTO dto = idToDto.get(e.getCentralId());
                if (!Objects.isNull(dto)) {
                    e.setPublished(dto.getPublished());
                    e.setModelType(dto.getModelType());
                }
            });
        } catch (RetryableException e) {
            LogUtils.logError(LOGGER, COMMUNICATION_FAILED, e);
            throw new ServiceNotAvailableException("Central is not available");
        } catch (AuthenticationException | FeignException.Unauthorized e) {
            LogUtils.logError(LOGGER, AUTH_ERROR_MESSAGE, e);
            throw new AuthException(e.getMessage());
        } catch (Exception e) {
            LogUtils.logError(LOGGER, UNEXPECTED_ERROR, e);
            throw new BadRequestException();
        }
        return dtos;
    }

    @ApiOperation(value = "Get data source")
    @RequestMapping(
            value = Constants.Api.DataSource.GET,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonResult<DataSourceDTO> get(Principal principal, @PathVariable("id") Long id) throws PermissionDeniedException {

        if (principal == null) {
            throw new AuthException("user not found");
        }
        JsonResult<DataSourceDTO> result = new JsonResult<>(NO_ERROR);
        DataSource dataSource = dataSourceService.getById(id);
        DataSourceDTO resultDTO = conversionService.convert(dataSource, DataSourceDTO.class);
        if (Objects.equals(dataNodeService.getDataNodeMode(), FunctionalMode.NETWORK)) {
            resultDTO = setFieldsFromCentral(getUser(principal), Collections.singletonList(resultDTO)).get(0);
        }
        result.setResult(resultDTO);
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
        JsonResult centralResult = dataSourceService.unpublishAndDeleteOnCentral(id);
        JsonResult<Boolean> result = new JsonResult<>();
        Integer centralErrorCode = centralResult.getErrorCode();

        if (asList(NO_ERROR.getCode(), VALIDATION_ERROR.getCode()).contains(centralErrorCode)) {
            if (VALIDATION_ERROR.getCode().equals(centralErrorCode)) {
                LOGGER.warn("Datasource with local id = {} cannot be found at central", id);
            }
            dataSourceService.delete(id);
            result.setErrorCode(NO_ERROR.getCode());
            result.setResult(Boolean.TRUE);
        } else {
            result.setErrorCode(centralErrorCode);
            result.setResult(Boolean.FALSE);
        }
        return result;
    }

    @ApiOperation(value = "Updates given data source")
    @RequestMapping(
            value = Constants.Api.DataSource.UPDATE,
            method = RequestMethod.PUT
    )
    public JsonResult<DataSourceDTO> update(Principal principal,
                                            @Valid @RequestPart("dataSource") CreateDataSourceDTO dataSourceDTO,
                                            @PathVariable("id") Long id,
                                            @RequestPart(name = "keyfile", required = false) MultipartFile keyfile,
                                            BindingResult bindingResult)
            throws PermissionDeniedException {

        if (bindingResult.hasErrors()) {
            return setValidationErrors(bindingResult);
        }
        final User user = getAdmin(principal);
        dataSourceDTO.setKeyfile(keyfile);
        DataSource dataSource = conversionService.convert(dataSourceDTO, DataSource.class);
        dataSource.setId(id);

        final DataSource savedDataSource = dataSourceService.update(user, dataSource);
        JsonResult<DataSourceDTO> result = new JsonResult<>(NO_ERROR);
        result.setResult(conversionService.convert(savedDataSource, DataSourceDTO.class));
        return result;
    }

    @ApiOperation("Remove kerberos keytab")
    @RequestMapping(
            value = Constants.Api.DataSource.DELETE_KEYTAB,
            method = RequestMethod.DELETE
    )
    public JsonResult removeKeytab(@PathVariable("id") Long id){

        DataSource dataSource = dataSourceService.getById(id);
        dataSourceService.removeKeytab(dataSource);
        return new JsonResult(NO_ERROR);
    }

    @ApiOperation("List supported DBMS")
    @RequestMapping(
            value = "/api/v1/data-sources/dbms-types",
            method = RequestMethod.GET
    )
    public List<OptionDTO> getDBMSTypes() {

        return converterUtils.convertList(asList(DBMSType.values()), OptionDTO.class);
    }

}
