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
 * Created: August 28, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.portal;

import static com.odysseusinc.arachne.datanode.Constants.CentralApi.DataNode.CREATION;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.DataSource.GET_LIST;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.DataSource.UPDATE;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.AUTH_METHOD;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.GET_USER;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.LOGIN;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.LOGOUT;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.PASSWORD_POLICIES;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.PROFESSIONAL_TYPES;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.REGISTRATION;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.SUGGEST;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.USER_INFO;

import com.odysseusinc.arachne.commons.api.v1.dto.ArachnePasswordInfoDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthMethodDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataNodeCreationResponseDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonProfessionalTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserRegistrationDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.commons.types.SuggestionTarget;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.user.CentralRegisterUserDTO;
import com.odysseusinc.arachne.datanode.dto.user.UserDTO;
import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;

public interface CentralClient {

    @RequestLine("DELETE /api/v1/data-sources/{dataSourceId}")
    JsonResult unpublishAndSoftDeleteDataSource(@Param("dataSourceId") Long dataSourceId);

    @RequestLine("POST /api/v1/auth/refresh")
    JsonResult<String> refreshToken(@HeaderMap Map<String, String> tokenHeader);

    @RequestLine("GET " + AUTH_METHOD)
    JsonResult<CommonAuthMethodDTO> getAuthMethod();

    @RequestLine("POST " + CREATION)
    JsonResult<CommonDataNodeCreationResponseDTO> sendDataNodeCreationRequest();

    @RequestLine("GET " + PASSWORD_POLICIES)
    ArachnePasswordInfoDTO getPasswordInfo();

    @RequestLine("POST " + REGISTRATION)
    void registerUser(CentralRegisterUserDTO userDTO);

    @RequestLine("POST " + LOGIN)
    @Headers("Content-Type: application/json")
    JsonResult login(CommonAuthenticationRequest authenticationRequest);

    @RequestLine("GET " + LOGOUT)
    JsonResult logout();

    @RequestLine("GET " + PROFESSIONAL_TYPES)
    JsonResult<CommonProfessionalTypeDTO> getProfessionalTypes();

    @RequestLine("GET " + USER_INFO)
    JsonResult<UserDTO> getUserInfo(@HeaderMap Map<String, Object> headers);

    @RequestLine("POST " + REGISTRATION)
    @Headers("Content-Type: application/json")
    JsonResult<CommonUserDTO> registerUser(CommonUserRegistrationDTO userRegistration);

    @RequestLine("GET " + GET_LIST + "?id={ids}")
    JsonResult<List<CommonDataSourceDTO>> getDataSources(@Param("ids") String ids);

    @RequestLine("GET " + SUGGEST + "?query={query}&target={target}&limit={limit}&excludeEmails={excludeEmails}")
    List<CommonUserDTO> suggestUsers(@Param("query") String query,
                                     @Param("target") SuggestionTarget target,
                                     @Param("limit") Integer limit,
                                     @Param("excludeEmails") String emails);

    @RequestLine("GET " + GET_USER)
    JsonResult<CommonUserDTO> getUser(@Param("id") Long centralUserId);

    @RequestLine("POST " + Constants.CentralApi.DataSource.CREATION)
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON_UTF8_VALUE)
    <T extends CommonDataSourceDTO> JsonResult<T> createDataSource(@Param("id") Long dataNodeId, T dataSourceDTO);

    @RequestLine("PUT " + UPDATE)
    <T extends CommonDataSourceDTO> JsonResult<T> updateDataSource(@Param("id") Long dataSourceId, T dataSource);
}
