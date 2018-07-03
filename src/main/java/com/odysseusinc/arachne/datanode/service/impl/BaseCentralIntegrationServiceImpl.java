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

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.NO_ERROR;
import static org.apache.commons.lang.StringUtils.isBlank;

import com.google.common.base.Functions;
import com.odysseusinc.arachne.commons.api.v1.dto.ArachnePasswordInfoDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthMethodDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataNodeCreationResponseDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonLinkUserToDataNodeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonProfessionalTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserRegistrationDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.commons.types.SuggestionTarget;
import com.odysseusinc.arachne.datanode.dto.user.CentralRegisterUserDTO;
import com.odysseusinc.arachne.datanode.dto.user.UserDTO;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.IntegrationValidationException;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.util.CentralUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseCentralIntegrationServiceImpl<DS extends DataSource, DTO extends CommonDataSourceDTO> implements BaseCentralIntegrationService<DS, DTO>, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(CentralIntegrationServiceImpl.class);
    protected final GenericConversionService conversionService;
    protected final CentralUtil centralUtil;
    protected CentralClient centralClient;
    protected CentralSystemClient centralSystemClient;
    protected final ApplicationContext applicationContext;

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
    }

    @Override
    public JsonResult<CommonAuthMethodDTO> getAuthMethod() {

        return centralClient.getAuthMethod();
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
        if (jsonResult == null || !JsonResult.ErrorCode.NO_ERROR.getCode().equals(jsonResult.getErrorCode())
                && !JsonResult.ErrorCode.VALIDATION_ERROR.getCode().equals(jsonResult.getErrorCode())) {
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
        if (jsonResult == null || !NO_ERROR.getCode().equals(jsonResult.getErrorCode())) {
            throw new IllegalStateException("Unable to create data source on central." + (jsonResult == null
                    ? "" : jsonResult.getErrorMessage()));
        }
        return jsonResult.getResult();
    }

    @Override
    public DTO sendDataSourceUpdateRequest(
            User user, Long centralDataSourceId,
            DTO commonCreateDataSourceDTO) {

        JsonResult<DTO> jsonResult = centralClient.updateDataSource(centralDataSourceId, commonCreateDataSourceDTO);
        if (jsonResult == null || !NO_ERROR.getCode().equals(jsonResult.getErrorCode())) {
            throw new IllegalStateException("Unable to update data source on central." + (jsonResult == null
                    ? "" : jsonResult.getErrorMessage()));
        }
        return jsonResult.getResult();
    }

    @Override
    public ArachnePasswordInfoDTO getPasswordInfo() {

        return centralClient.getPasswordInfo();
    }

    @Override
    public void registerUserOnCentral(CentralRegisterUserDTO registerUserDTO) {

        try {
            centralClient.registerUser(registerUserDTO);
        } catch (Exception ex) {
            throw new AuthException("unable to register user on central " + ex.getMessage());
        }
    }

    @Override
    public String loginToCentral(String username, String password) {

        JsonResult result = centralClient.login(new CommonAuthenticationRequest(username, password));

        if (result == null) {
            throw new AuthException("Empty response body");
        }

        if (result.getErrorCode() != 0) {
            throw new AuthException(result.getErrorMessage());
        }

        if (result.getResult() == null) {
            throw new AuthException("Missing JWT token from central");
        }

        return ((Map) result.getResult()).get("token").toString();
    }

    @Override
    public User getUserInfoFromCentral(String centralToken) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(authHeader, centralToken);
        JsonResult<UserDTO> userDTO = centralClient.getUserInfo(headers);
        return conversionService.convert(userDTO.getResult(), User.class);
    }

    @Override
    public JsonResult<CommonProfessionalTypeDTO> getProfessionalTypes() {

        return centralClient.getProfessionalTypes();
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
    public JsonResult<CommonUserDTO> getUserFromCentral(User user, Long centralUserId) {

        return centralClient.getUser(centralUserId);
    }

    @Override
    public void linkUserToDataNodeOnCentral(DataNode dataNode, User user) {

        final CommonLinkUserToDataNodeDTO commonLinkUserToDataNode
                = conversionService.convert(user, CommonLinkUserToDataNodeDTO.class);
        centralSystemClient.linkUser(dataNode.getCentralId(), commonLinkUserToDataNode);
    }

    @Override
    public void unlinkUserToDataNodeOnCentral(DataNode dataNode, User user) {

        final CommonLinkUserToDataNodeDTO commonLinkUserToDataNode
                = conversionService.convert(user, CommonLinkUserToDataNodeDTO.class);
        centralSystemClient.unlinkUser(dataNode.getCentralId(), commonLinkUserToDataNode);
    }

    @Override
    @Transactional
    public List<User> relinkAllUsersToDataNodeOnCentral(DataNode dataNode, List<User> users) {

        final List<CommonLinkUserToDataNodeDTO> commonLinkUserToDataNodes = users.stream()
                .map(user -> conversionService.convert(user, CommonLinkUserToDataNodeDTO.class))
                .collect(Collectors.toList());
        List<CommonUserDTO> linkedUsers = centralSystemClient.relinkUsers(dataNode.getCentralId(), commonLinkUserToDataNodes)
                .getResult();
        return linkedUsers.stream().map(user -> conversionService.convert(user, User.class)).collect(Collectors.toList());
    }

}
