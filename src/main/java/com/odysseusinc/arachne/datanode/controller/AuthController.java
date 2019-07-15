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
 * Created: December 16, 2016
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.odysseusinc.arachne.commons.api.v1.dto.ArachnePasswordInfoDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthMethodDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationResponse;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCountryDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonProfessionalTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonStateProvinceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserRegistrationDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.dto.user.UserInfoDTO;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.security.TokenUtils;
import com.odysseusinc.arachne.datanode.service.CentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.UserService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import io.swagger.annotations.ApiOperation;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AuthController {
    protected Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private CentralIntegrationService integrationService;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private CentralClient centralClient;

    @Value("${datanode.jwt.header}")
    private String tokenHeader;

    @ApiOperation("Get auth method")
    @RequestMapping(value = "/api/v1/auth/method", method = GET)
    public JsonResult<CommonAuthMethodDTO> authMethod() {

        return integrationService.getAuthMethod();
    }

    @ApiOperation(value = "Sign in user. Returns JWT token.")
    @RequestMapping(value = "${api.loginEnteryPoint}", method = RequestMethod.POST)
    public JsonResult<CommonAuthenticationResponse> login(
            @RequestBody CommonAuthenticationRequest authenticationRequest) {

        String username = authenticationRequest.getUsername();
        String centralToken = integrationService.loginToCentral(username, authenticationRequest.getPassword());
        validateCentralTokenCreated(centralToken);
        User centralUser = integrationService.getUserInfoFromCentral(centralToken);
        User user = userService.findByUsername(username).orElseGet(() -> userService.createIfFirst(centralUser));
        userService.updateUserInfo(centralUser);
        userService.setToken(user, centralToken);
        String notSignedToken = centralToken.substring(0, centralToken.lastIndexOf('.') + 1);
        Date createdDateFromToken = tokenUtils.getCreatedDateFromToken(notSignedToken, false);
        final String nodeToken = tokenUtils.generateToken(user, createdDateFromToken);

        JsonResult<CommonAuthenticationResponse> jsonResult = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        CommonAuthenticationResponse authenticationResponse = new CommonAuthenticationResponse(nodeToken);
        jsonResult.setResult(authenticationResponse);
        return jsonResult;
    }

    @ApiOperation("Refresh session token.")
    @RequestMapping(value = "/api/v1/auth/refresh", method = RequestMethod.POST)
    public JsonResult<String> refresh(HttpServletRequest request) {
        User user = extractUser(request);
        String currentCentralToken = user.getToken();
        Map<String, String> header = Collections.singletonMap(this.tokenHeader, currentCentralToken);
        String newCentralToken = centralClient.refreshToken(header).getResult();
        validateCentralTokenCreated(newCentralToken);
        userService.setToken(user, newCentralToken);
        String notSignedToken = newCentralToken.substring(0, newCentralToken.lastIndexOf('.') + 1);
        Date createdDateFromToken = tokenUtils.getCreatedDateFromToken(notSignedToken, false);
        String newNodeToken = tokenUtils.generateToken(user, createdDateFromToken);

        JsonResult<String> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        result.setResult(newNodeToken);
        return result;
    }

    @ApiOperation("Get current principal")
    @RequestMapping(value = "/api/v1/auth/me", method = GET)
    public JsonResult principal(Principal principal) {

        JsonResult result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);

        userService
                .findByUsername(principal.getName())
                .ifPresent(user -> {
                    UserInfoDTO userInfoDTO = new UserInfoDTO();
                    userInfoDTO.setUsername(user.getEmail());
                    final boolean isAdmin = user.getRoles().stream()
                            .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
                    userInfoDTO.setIsAdmin(isAdmin);
                    userInfoDTO.setFirstname(user.getFirstName());
                    userInfoDTO.setLastname(user.getLastName());
                    result.setResult(userInfoDTO);
                });

        return result;
    }

    @ApiOperation("Logout")
    @RequestMapping(value = "/api/v1/auth/logout", method = RequestMethod.POST)
    public JsonResult logout(HttpServletRequest request) {

        JsonResult result;
        try {
            String token = request.getHeader(tokenHeader);
            if (token != null) {
                tokenUtils.addInvalidateToken(token);
            }
            result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
            result.setResult(true);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            result = new JsonResult<>(JsonResult.ErrorCode.SYSTEM_ERROR);
            result.setResult(false);
            result.setErrorMessage(ex.getMessage());
        }
        return result;
    }

    @ApiOperation(value = "Get professional types list")
    @RequestMapping(value = "/api/v1/user-management/professional-types", method = GET)
    public JsonResult<List<CommonProfessionalTypeDTO>> getProfessionalTypes() {

        return integrationService.getProfessionalTypes();
    }

    @ApiOperation("Suggests country.")
    @RequestMapping(value = "/api/v1/user-management/countries/search", method = GET)
    public JsonResult<List<CommonCountryDTO>> suggestCountries(
            @RequestParam("query") String query,
            @RequestParam("limit") Integer limit,
            @RequestParam(value = "includeId", required = false) Long includeId

    ) {

        return integrationService.getCountries(query, limit, includeId);
    }

    @ApiOperation("Suggests state or province.")
    @RequestMapping(value = "/api/v1/user-management/state-province/search", method = GET)
    public JsonResult<List<CommonStateProvinceDTO>> suggestStateProvince(
            @RequestParam("countryId") String countryId,
            @RequestParam("query") String query,
            @RequestParam("limit") Integer limit,
            @RequestParam(value = "includeId", required = false) String includeId
    ) {

        return integrationService.getStateProvinces(countryId, query, limit, includeId);
    }

    @ApiOperation("Register new user via form.")
    @RequestMapping(value = "/api/v1/auth/registration", method = RequestMethod.POST)
    public JsonResult<CommonUserDTO> register(@RequestBody CommonUserRegistrationDTO dto) {
        return integrationService.getRegisterUser(dto);
    }

    @ApiOperation("Password restrictions")
    @RequestMapping(value = "/api/v1/auth/password-policies", method = GET)
    public ArachnePasswordInfoDTO getPasswordPolicies() throws URISyntaxException {

        return integrationService.getPasswordInfo();
    }

    private User extractUser(HttpServletRequest request) {
        String currentNodeToken = request.getHeader(this.tokenHeader);
        return userService.findByUsername(tokenUtils.getUsernameFromToken(currentNodeToken))
                .orElseThrow(() -> new AuthException("user not registered"));
    }

    private void validateCentralTokenCreated(String centralToken) {
        if (centralToken == null) {
            throw new AuthException("central auth error");
        }
    }
}
