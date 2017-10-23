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
 * Created: December 16, 2016
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthMethodDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationResponse;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonProfessionalTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserRegistrationDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.dto.user.UserInfoDTO;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.security.TokenUtils;
import com.odysseusinc.arachne.datanode.service.CentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.security.Principal;
import java.util.Date;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

//import static com.odysseusinc.arachne.datanode.Constants.Api.Auth.LOGIN_ENTRY_POINT;

@Api
@RestController
public class AuthController {
    protected Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private CentralIntegrationService integrationService;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private UserService userService;

    @Value("datanode.jwt.header")
    private String tokenHeader;

    @ApiOperation("Get auth method")
    @RequestMapping(value = "/api/v1/auth/method", method = RequestMethod.GET)
    public JsonResult<CommonAuthMethodDTO> authMethod() {

        return integrationService.getAuthMethod();
    }

    @ApiOperation(value = "Sign in user. Returns JWT token.")
    @RequestMapping(value = "${api.loginEnteryPoint}", method = RequestMethod.POST)
    public JsonResult<CommonAuthenticationResponse> login(
            @RequestBody CommonAuthenticationRequest authenticationRequest)
            throws AuthenticationException {

        JsonResult<CommonAuthenticationResponse> jsonResult;
        try {
            final String token;
            String username = authenticationRequest.getUsername();
            Optional<User> byUsername = userService.findByUsername(username);
            User user;
            if (!byUsername.isPresent()) {
                user = userService.createIfFirst(username);
                if (user == null) {
                    throw new AuthException("user not registered");
                }
            } else {
                user = byUsername.get();
            }
            String centralToken = integrationService.loginToCentral(username, authenticationRequest.getPassword());
            if (centralToken == null) {
                throw new AuthException("central auth error");
            }
            userService.setToken(user, centralToken);
            String notSignedToken = centralToken.substring(0, centralToken.lastIndexOf(".") + 1);
            Date createdDateFromToken = tokenUtils.getCreatedDateFromToken(notSignedToken, false);
            token = tokenUtils.generateToken(user, createdDateFromToken);

            jsonResult = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
            CommonAuthenticationResponse authenticationResponse = new CommonAuthenticationResponse(token);
            jsonResult.setResult(authenticationResponse);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            jsonResult = new JsonResult<>(JsonResult.ErrorCode.UNAUTHORIZED);
            jsonResult.setErrorMessage(ex.getMessage());
        }
        // Return the token
        return jsonResult;
    }

    @ApiOperation("Get current principal")
    @RequestMapping(value = "/api/v1/auth/me", method = RequestMethod.GET)
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
    @RequestMapping(value = "/api/v1/user-management/professional-types", method = RequestMethod.GET)
    public JsonResult<CommonProfessionalTypeDTO> getProfessionalTypes() {

        return integrationService.getProfessionalTypes();
    }


    @ApiOperation("Register new user via form.")
    @RequestMapping(value = "/api/v1/auth/registration", method = RequestMethod.POST)
    public JsonResult<CommonUserDTO> register(@RequestBody CommonUserRegistrationDTO dto) throws Exception {

        return integrationService.getRegisterUser(dto);

    }

}
