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
 * Created: September 25, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.google.common.base.Functions;
import com.odysseusinc.arachne.commons.api.v1.dto.ArachnePasswordInfoDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthMethodDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCountryDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataNodeCreationResponseDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonLinkUserToDataNodeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonProfessionalTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonStateProvinceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserRegistrationDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.commons.types.SuggestionTarget;
import com.odysseusinc.arachne.datanode.dto.user.RemindPasswordDTO;
import com.odysseusinc.arachne.datanode.exception.IntegrationValidationException;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.util.CentralUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.GenericConversionService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.NO_ERROR;
import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.VALIDATION_ERROR;
import static com.odysseusinc.arachne.datanode.Constants.DataSourceMessages.CANNOT_CREATE_DATASOURCE;
import static com.odysseusinc.arachne.datanode.Constants.DataSourceMessages.CANNOT_UPDATE_DATASOURCE;
import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class BaseCentralIntegrationServiceImpl<DS extends DataSource, DTO extends CommonDataSourceDTO> implements BaseCentralIntegrationService<DS, DTO>, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(BaseCentralIntegrationServiceImpl.class);
    protected final GenericConversionService conversionService;
    protected final CentralUtil centralUtil;
    protected CentralClient centralClient;
    protected CentralSystemClient centralSystemClient;
    protected final ApplicationContext applicationContext;
    protected DataNodeService dataNodeService;

    @Value("${arachne.token.header}")
    private String authHeader;

    public BaseCentralIntegrationServiceImpl(GenericConversionService conversionService,
                                             CentralUtil centralUtil,
                                             ApplicationContext applicationContext) {

        this.conversionService = conversionService;
        this.centralUtil = centralUtil;
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        centralClient = applicationContext.getBean(CentralClient.class);
        centralSystemClient = applicationContext.getBean(CentralSystemClient.class);
        dataNodeService = applicationContext.getBean(DataNodeService.class);
    }

    @Override
    public JsonResult<CommonAuthMethodDTO> getAuthMethod() {

        if (dataNodeService.isNetworkMode()) {
            return centralClient.getAuthMethod();
        } else {
            return new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        }
    }

    @Override
    public DataNode sendDataNodeCreationRequest(User user, DataNode dataNode) {

        return sendDataNodeRequestEntity(dataNode);
    }

    private DataNode sendDataNodeRequestEntity(DataNode dataNode) {

        JsonResult<CommonDataNodeCreationResponseDTO> jsonResult = centralClient.sendDataNodeCreationRequest();
        if (jsonResult != null && JsonResult.ErrorCode.VALIDATION_ERROR.getCode().equals(jsonResult.getErrorCode())) {
            throw new IntegrationValidationException(jsonResult);
        }
        if (jsonResult == null || !JsonResult.ErrorCode.NO_ERROR.hasEqualCode(jsonResult)
                && !JsonResult.ErrorCode.VALIDATION_ERROR.hasEqualCode(jsonResult)) {
            throw new IllegalStateException("Unable to register data node on central." + (jsonResult == null
                    ? "" : jsonResult.getErrorMessage()));
        }
        final CommonDataNodeCreationResponseDTO responseDTO = jsonResult.getResult();
        final String token = responseDTO.getToken();
        if (isBlank(token)) {
            throw new IllegalStateException("Unable to register data node on central. Token is blank");
        } else {
            dataNode.setCentralId(responseDTO.getCentralId());
            dataNode.setToken(token);
        }
        return dataNode;
    }

    @Override
    public DTO sendDataSourceCreationRequest(
            User user, DataNode dataNode,
            DTO commonCreateDataSourceDTO) {

        JsonResult<DTO> jsonResult = centralClient.createDataSource(dataNode.getCentralId(), commonCreateDataSourceDTO);
        validatePersistDataSourceResult(jsonResult, CANNOT_CREATE_DATASOURCE);
        return jsonResult.getResult();
    }

    @Override
    public DTO sendDataSourceUpdateRequest(
            User user, Long centralDataSourceId,
            DTO commonCreateDataSourceDTO) {

        JsonResult<DTO> jsonResult = centralClient.updateDataSource(centralDataSourceId, commonCreateDataSourceDTO);
        validatePersistDataSourceResult(jsonResult, CANNOT_UPDATE_DATASOURCE);
        return jsonResult.getResult();
    }

    private void validatePersistDataSourceResult(JsonResult<DTO> jsonResult, String errorMessageTemplate) {

        if (NO_ERROR.hasEqualCode(jsonResult)) {
            return;
        }

        if (jsonResult == null) {
            throw new IllegalStateException(errorMessageTemplate);
        }

        if (VALIDATION_ERROR.hasEqualCode(jsonResult)) {
            throw new IntegrationValidationException(jsonResult);
        }

        throw new IllegalStateException(errorMessageTemplate + jsonResult.getErrorMessage());
    }

    @Override
    public ArachnePasswordInfoDTO getPasswordInfo() {

        return centralClient.getPasswordInfo();
    }

    @Override
    public JsonResult<List<CommonProfessionalTypeDTO>> getProfessionalTypes() {

        return centralClient.getProfessionalTypes();
    }

    @Override
    public JsonResult<List<CommonCountryDTO>> getCountries(String query, Integer limit, Long includeId) {

        return centralClient.getCountries(query, limit, includeId);
    }

    @Override
    public JsonResult<List<CommonStateProvinceDTO>> getStateProvinces(String countryId, String query, Integer limit, String includeId) {

        return centralClient.getStateProvinces(countryId, query, limit, includeId);
    }

    @Override
    public JsonResult<CommonUserDTO> getRegisterUser(CommonUserRegistrationDTO dto) {

        return centralClient.registerUser(dto);
    }

    @Override
    public JsonResult<DTO> getDataSource(User user, Long id) {

        return centralSystemClient.getDataSource(id);
    }

    @Override
    public JsonResult<List<CommonDataSourceDTO>> getDataSources(User user, List<Long> ids) {

        return centralClient.getDataSources(ids.stream().map(Functions.toStringFunction()::apply)
                .collect(Collectors.joining(",")));
    }

    @Override
    public List<CommonUserDTO> suggestUsersFromCentral(
            User user,
            String query,
            Set<String> emails,
            int limit) {

        return centralClient.suggestUsers(query, SuggestionTarget.DATANODE, limit,
                emails.stream().collect(Collectors.joining(",")));
    }

    @Override
    public JsonResult<CommonUserDTO> getUserFromCentral(User user, String username) {

        try {
            return centralClient.getUser(URLEncoder.encode(username, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void linkUserToDataNodeOnCentral(DataNode dataNode, User user) {

        final CommonLinkUserToDataNodeDTO commonLinkUserToDataNode
                = conversionService.convert(user, CommonLinkUserToDataNodeDTO.class);
        centralSystemClient.linkUser(dataNode.getCentralId(), commonLinkUserToDataNode);
    }

    @Override
    public void unlinkUserToDataNodeOnCentral(DataNode dataNode, User user) {

        final CommonLinkUserToDataNodeDTO commonLinkUserToDataNode = conversionService.convert(user, CommonLinkUserToDataNodeDTO.class);
        centralSystemClient.unlinkUser(dataNode.getCentralId(), commonLinkUserToDataNode);
    }

    @Override
    public void relinkUsersToDataNodeOnCentral(DataNode dataNode, List<User> unlinkedUsers) {

        for (User unlinkedUser : unlinkedUsers) {
            try {
                linkUserToDataNodeOnCentral(dataNode, unlinkedUser);
                unlinkedUser.setSync(true);
                log.info("linking user {} to the Central", unlinkedUser.getUsername());
            } catch (Exception ex) {
                log.warn("Disabling user [{}] as it can't be linked to central due to error: [{}]", unlinkedUser.getUsername(), ex.getMessage());
                log.info(ex.getClass().getName(), ex);
                unlinkedUser.setEnabled(false);
            }
        }
    }

    @Override
    public void remindPassword(RemindPasswordDTO remindPasswordDTO) {

        log.debug("remind password for: {}", remindPasswordDTO.getEmail());
        centralClient.remindPassword(remindPasswordDTO);
    }
}
