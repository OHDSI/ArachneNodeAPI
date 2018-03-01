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
 * Created: September 25, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import static org.apache.commons.lang.StringUtils.isBlank;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.odysseusinc.arachne.commons.api.v1.dto.ArachnePasswordInfoDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthMethodDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataNodeCreationResponseDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataNodeRegisterDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonLinkUserToDataNodeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonProfessionalTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserRegistrationDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.user.CentralRegisterUserDTO;
import com.odysseusinc.arachne.datanode.dto.user.UserDTO;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.IntegrationValidationException;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.util.CentralUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class BaseCentralIntegrationServiceImpl<DS extends DataSource, DTO extends CommonDataSourceDTO> implements BaseCentralIntegrationService<DS, DTO> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CentralIntegrationServiceImpl.class);
    protected final RestTemplate centralRestTemplate;
    protected final GenericConversionService conversionService;
    protected final CentralUtil centralUtil;

    @Value("${arachne.token.header}")
    private String authHeader;

    public BaseCentralIntegrationServiceImpl(GenericConversionService conversionService, @Qualifier("centralRestTemplate") RestTemplate centralRestTemplate, CentralUtil centralUtil) {

        this.conversionService = conversionService;
        this.centralRestTemplate = centralRestTemplate;
        this.centralUtil = centralUtil;
    }

    @Override
    public JsonResult<CommonAuthMethodDTO> getAuthMethod() {

        String url = centralUtil.getCentralUrl() + Constants.CentralApi.User.AUTH_METHOD;
        final ParameterizedTypeReference<JsonResult<CommonAuthMethodDTO>> parameterizedTypeReference
                = new ParameterizedTypeReference<JsonResult<CommonAuthMethodDTO>>() {
        };
        final ResponseEntity<JsonResult<CommonAuthMethodDTO>> exchange = centralRestTemplate.exchange(url, HttpMethod.GET, null, parameterizedTypeReference);
        return exchange.getBody();
    }

    @Override
    public DataNode sendDataNodeCreationRequest(User user, DataNode dataNode) {

        String url = centralUtil.getCentralUrl() + Constants.CentralApi.DataNode.CREATION;
        return sendDataNodeRequestEntity(user, dataNode, url, HttpMethod.POST);
    }

    private DataNode sendDataNodeRequestEntity(User user, DataNode dataNode, String url, HttpMethod method,
                                               String... uriVariables) {

        final HttpEntity<CommonDataNodeRegisterDTO> requestEntity
                = new HttpEntity<>(centralUtil.getCentralAuthHeader(user.getToken()));
        final ParameterizedTypeReference<JsonResult<CommonDataNodeCreationResponseDTO>> responseType
                = new ParameterizedTypeReference<JsonResult<CommonDataNodeCreationResponseDTO>>() {
        };
        ResponseEntity<JsonResult<CommonDataNodeCreationResponseDTO>> responseEntity
                = centralRestTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
        JsonResult<CommonDataNodeCreationResponseDTO> jsonResult = responseEntity.getBody();
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
    public JsonResult<DTO> sendDataSourceCreationRequest(
            User user, DataNode dataNode,
            DTO commonCreateDataSourceDTO) {

        String url = centralUtil.getCentralUrl() + Constants.CentralApi.DataSource.CREATION;
        Map<String, Object> uriParams = new HashMap<>();
        uriParams.put("id", dataNode.getCentralId());
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);

        HttpEntity<DTO> requestEntity =
                new HttpEntity<>(commonCreateDataSourceDTO, centralUtil.getCentralAuthHeader(user.getToken()));
        ResponseEntity<JsonResult<DTO>> exchange =
                centralRestTemplate.exchange(
                        uriBuilder.buildAndExpand(uriParams).toUri(),
                        HttpMethod.POST, requestEntity,
                        getParameterizedTypeReferenceJsonResultDTO());

        return exchange.getBody();
    }

    @Override
    public JsonResult<DTO> sendDataSourceUpdateRequest(
            User user, DataSource dataSource,
            DTO commonCreateDataSourceDTO) {

        String url = centralUtil.getCentralUrl() + Constants.CentralApi.DataSource.UPDATE;
        Map<String, Object> uriParams = new HashMap<>();
        uriParams.put("id", dataSource.getCentralId());
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);

        HttpEntity<DTO> requestEntity =
                new HttpEntity<>(commonCreateDataSourceDTO, centralUtil.getCentralAuthHeader(user.getToken()));
        ResponseEntity<JsonResult<DTO>> exchange =
                centralRestTemplate.exchange(
                        uriBuilder.buildAndExpand(uriParams).toUri(),
                        HttpMethod.PUT, requestEntity,
                        getParameterizedTypeReferenceJsonResultDTO());

        return exchange.getBody();
    }

    @Override
    public ArachnePasswordInfoDTO getPasswordInfo() throws URISyntaxException {

        return centralRestTemplate.getForEntity(new URI(centralUtil.getCentralUrl()
                + Constants.CentralApi.User.PASSWORD_POLICIES), ArachnePasswordInfoDTO.class)
                .getBody();
    }

    @Override
    public void registerUserOnCentral(CentralRegisterUserDTO registerUserDTO) {

        try {
            centralRestTemplate.exchange(
                    new URI(centralUtil.getCentralUrl()
                            + Constants.CentralApi.User.REGISTRATION),
                    HttpMethod.POST,
                    new HttpEntity(registerUserDTO), Object.class);
        } catch (URISyntaxException ex) {
            throw new AuthException("unable to register user on central " + ex.getMessage());
        }
    }

    @Override
    public String loginToCentral(String username, String password) {

        HttpEntity<CommonAuthenticationRequest> request = new HttpEntity<>(new CommonAuthenticationRequest(username, password));
        ResponseEntity<JsonResult> resultEntity =
                centralRestTemplate.postForEntity(
                        centralUtil.getCentralUrl() + Constants.CentralApi.User.LOGIN,
                        request,
                        JsonResult.class);
        JsonResult result = resultEntity.getBody();

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

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(authHeader, centralToken);
        HttpEntity request = new HttpEntity(headers);
        String url = centralUtil.getCentralUrl() + Constants.CentralApi.User.USER_INFO;
        ResponseEntity<JsonResult<UserDTO>> exchange =
                centralRestTemplate.exchange(url, HttpMethod.GET, request,
                        new ParameterizedTypeReference<JsonResult<UserDTO>>() {
                        });
        return conversionService.convert(exchange.getBody().getResult(), User.class);
    }

    @Override
    public JsonResult<CommonProfessionalTypeDTO> getProfessionalTypes() {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        HttpEntity request = new HttpEntity(headers);
        String url = centralUtil.getCentralUrl() + Constants.CentralApi.User.PROFESSIONAL_TYPES;
        ResponseEntity<JsonResult> exchange =
                centralRestTemplate.exchange(url, HttpMethod.GET, request, JsonResult.class);
        return (JsonResult<CommonProfessionalTypeDTO>) exchange.getBody();
    }

    @Override
    public JsonResult<CommonUserDTO> getRegisterUser(CommonUserRegistrationDTO dto) {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        HttpEntity request = new HttpEntity(dto, headers);
        String url = centralUtil.getCentralUrl() + Constants.CentralApi.User.REGISTER_USER;
        ResponseEntity<JsonResult> exchange = centralRestTemplate.exchange(
                url, HttpMethod.POST, request, JsonResult.class);
        return (JsonResult<CommonUserDTO>) exchange.getBody();
    }

    @Override
    public JsonResult<DTO> getDataSource(User user, Long id) {

        String url = centralUtil.getCentralUrl() + Constants.CentralApi.DataSource.GET;
        Map<String, Object> uriParams = new HashMap<>();
        uriParams.put("id", id);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);

        HttpEntity request = new HttpEntity<>(centralUtil.getCentralAuthHeader(user.getToken()));

        ResponseEntity<JsonResult<DTO>> exchange = centralRestTemplate.exchange(
                uriBuilder.buildAndExpand(uriParams).toUri(),
                HttpMethod.GET,
                request,
                getParameterizedTypeReferenceJsonResultDTO()
        );
        return exchange.getBody();
    }

    @Override
    public JsonResult<List<CommonDataSourceDTO>> getDataSources(User user, List<Long> ids) {

        String url = centralUtil.getCentralUrl() + Constants.CentralApi.DataSource.GET_LIST;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
        uriBuilder.queryParam("id", String.join(",", Lists.transform(ids, Functions.toStringFunction())));

        HttpEntity request = new HttpEntity<>(centralUtil.getCentralAuthHeader(user.getToken()));
        ParameterizedTypeReference<JsonResult<List<CommonDataSourceDTO>>> responseType
                = new ParameterizedTypeReference<JsonResult<List<CommonDataSourceDTO>>>() {
        };
        ResponseEntity<JsonResult<List<CommonDataSourceDTO>>> exchange = centralRestTemplate.exchange(
                uriBuilder.buildAndExpand().toUri(),
                HttpMethod.GET,
                request,
                responseType
        );
        return exchange.getBody();
    }

    @Override
    public JsonResult<List<CommonUserDTO>> suggestUsersFromCentral(
            User user,
            String query,
            Set<String> emails,
            int limit) {

        UriComponentsBuilder uriBuilder =
                UriComponentsBuilder.fromUriString(centralUtil.getCentralUrl()
                        + Constants.CentralApi.User.SUGGEST);
        uriBuilder.queryParam("query", query);
        uriBuilder.queryParam("limit", Integer.toString(limit));
        for (String email : emails) {
            uriBuilder.queryParam("email", email);
        }
        HttpEntity request = new HttpEntity<>(centralUtil.getCentralAuthHeader(user.getToken()));
        ParameterizedTypeReference<JsonResult<List<CommonUserDTO>>> responseType
                = new ParameterizedTypeReference<JsonResult<List<CommonUserDTO>>>() {
        };
        ResponseEntity<JsonResult<List<CommonUserDTO>>> exchange = centralRestTemplate.exchange(
                uriBuilder.buildAndExpand().toUri(),
                HttpMethod.GET, request, responseType);
        return exchange.getBody();
    }

    @Override
    public JsonResult<CommonUserDTO> getUserFromCentral(User user, Long centralUserId) {

        UriComponentsBuilder uriBuilder =
                UriComponentsBuilder.fromUriString(centralUtil.getCentralUrl()
                        + Constants.CentralApi.User.GET_USER);
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("id", centralUserId.toString());

        HttpEntity request = new HttpEntity<>(centralUtil.getCentralAuthHeader(user.getToken()));
        ParameterizedTypeReference<JsonResult<CommonUserDTO>> responseType
                = new ParameterizedTypeReference<JsonResult<CommonUserDTO>>() {
        };
        ResponseEntity<JsonResult<CommonUserDTO>> exchange = centralRestTemplate.exchange(
                uriBuilder.buildAndExpand(uriParams).toUri(),
                HttpMethod.GET, request, responseType);
        return exchange.getBody();
    }

    @Override
    public void linkUserToDataNodeOnCentral(DataNode dataNode, User user) {

        final HttpHeaders headers = centralUtil.getCentralNodeAuthHeader(dataNode.getToken());
        final CommonLinkUserToDataNodeDTO commonLinkUserToDataNode
                = conversionService.convert(user, CommonLinkUserToDataNodeDTO.class);
        final HttpEntity<CommonLinkUserToDataNodeDTO> httpEntity = new HttpEntity<>(commonLinkUserToDataNode, headers);
        linkUnlinkDataNodeUsers(dataNode.getCentralId(), httpEntity, HttpMethod.POST);
    }

    @Override
    public void unlinkUserToDataNodeOnCentral(DataNode dataNode, User user) {

        final HttpHeaders headers = centralUtil.getCentralNodeAuthHeader(dataNode.getToken());
        final CommonLinkUserToDataNodeDTO commonLinkUserToDataNode
                = conversionService.convert(user, CommonLinkUserToDataNodeDTO.class);
        final HttpEntity<CommonLinkUserToDataNodeDTO> httpEntity = new HttpEntity<>(commonLinkUserToDataNode, headers);
        linkUnlinkDataNodeUsers(dataNode.getCentralId(), httpEntity, HttpMethod.DELETE);
    }

    @Override
    @Transactional
    public List<User> relinkAllUsersToDataNodeOnCentral(DataNode dataNode, List<User> users) {

        final HttpHeaders headers = centralUtil.getCentralNodeAuthHeader(dataNode.getToken());
        final List<CommonLinkUserToDataNodeDTO> commonLinkUserToDataNodes = users.stream()
                .map(user -> conversionService.convert(user, CommonLinkUserToDataNodeDTO.class))
                .collect(Collectors.toList());
        final HttpEntity<List<CommonLinkUserToDataNodeDTO>> httpEntity
                = new HttpEntity<>(commonLinkUserToDataNodes, headers);
        List<CommonUserDTO> linkedUsers = linkUnlinkDataNodeUsers(dataNode.getCentralId(), httpEntity, HttpMethod.PUT);
        return linkedUsers.stream().map(user -> conversionService.convert(user, User.class)).collect(Collectors.toList());
    }

    private List<CommonUserDTO> linkUnlinkDataNodeUsers(Long datanodeId, HttpEntity httpEntity, HttpMethod method) {

        final String uri = centralUtil.getCentralUrl() + Constants.CentralApi.User.LINK_TO_NODE;
        final ResponseEntity<JsonResult<List<CommonUserDTO>>> response = centralRestTemplate.exchange(
                uri,
                method,
                httpEntity,
                new ParameterizedTypeReference<JsonResult<List<CommonUserDTO>>>() {
                },
                datanodeId
        );
        final JsonResult result = response.getBody();
        if (result.getErrorCode() != 0) {
            throw new IllegalStateException(result.getErrorMessage());
        }
        return (List<CommonUserDTO>) result.getResult();
    }

    protected void logoutFromCentral(String token) {

        try {
            centralRestTemplate.exchange(
                    new URI(centralUtil.getCentralUrl() + Constants.CentralApi.User.LOGOUT),
                    HttpMethod.GET, new HttpEntity(centralUtil.getCentralAuthHeader(token)), JsonResult.class);
        } catch (URISyntaxException ex) {
            throw new AuthException("unable to logout from central " + ex.getMessage());
        }
    }

    protected abstract ParameterizedTypeReference<JsonResult<DTO>> getParameterizedTypeReferenceJsonResultDTO();
}
