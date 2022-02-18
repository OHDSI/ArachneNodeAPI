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

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.NO_ERROR;
import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.UNAUTHORIZED;
import static com.odysseusinc.arachne.datanode.util.RestUtils.requireNetworkMode;
import static io.netty.handler.codec.http.cookie.CookieHeaderNames.SameSite;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.odysseusinc.arachne.commons.api.v1.dto.ArachnePasswordInfoDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthMethodDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationModeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationResponse;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCountryDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonProfessionalTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonStateProvinceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserRegistrationDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.dto.user.RemindPasswordDTO;
import com.odysseusinc.arachne.datanode.dto.user.UserInfoDTO;
import com.odysseusinc.arachne.datanode.exception.BadRequestException;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.CentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.UserRegistrationStrategy;
import com.odysseusinc.arachne.datanode.service.UserService;
import io.swagger.annotations.ApiOperation;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.authenticator.model.UserInfo;
import org.ohdsi.authenticator.service.authentication.AccessTokenResolver;
import org.ohdsi.authenticator.service.authentication.AuthenticationMode;
import org.ohdsi.authenticator.service.authentication.Authenticator;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
    private UserService userService;

    @Autowired
    private Authenticator authenticator;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private DataNodeService dataNodeService;

    @Autowired
    private UserRegistrationStrategy userRegisterStrategy;

    @Autowired
    private AccessTokenResolver accessTokenResolver;

    @Value("${datanode.jwt.header}")
    private String tokenHeader;

    @Value("${security.method}")
    private String authMethod;

    @Value("${security.authentication.mode:" + AuthenticationMode.Const.STANDARD + "}")
    private AuthenticationMode authenticationMode = AuthenticationMode.STANDARD;

    /**
     * @return
     * @deprecated not required as authenticator was implemented that provides authentication source
     * in a very flexible way.
     */
    @ApiOperation("Get auth method")
    @RequestMapping(value = "/api/v1/auth/method", method = GET)
    @Deprecated
    public JsonResult<CommonAuthMethodDTO> authMethod() {

        if (dataNodeService.isNetworkMode()) {
            return integrationService.getAuthMethod();
        }
        throw new BadRequestException();
    }


    @ApiOperation("Get authentication mode")
    @RequestMapping(value = "/api/v1/auth/mode", method = GET)
    @Deprecated
    public JsonResult<CommonAuthenticationModeDTO> authenticationMode() {

        return new JsonResult<>(JsonResult.ErrorCode.NO_ERROR, new CommonAuthenticationModeDTO(authenticationMode.getValue()));
    }

    @ApiOperation(value = "Sign in user. Returns JWT token.")
    @RequestMapping(value = "${api.loginEnteryPoint}", method = RequestMethod.POST)
    public ResponseEntity<JsonResult<?>> login(
            @Valid @RequestBody CommonAuthenticationRequest request) {

        UserInfo userInfo = authenticator.authenticate(
                authMethod,
                new UsernamePasswordCredentials(request.getUsername(), request.getPassword())
        );
        User centralUser = conversionService.convert(userInfo, User.class);
        userRegisterStrategy.registerUser(centralUser);

        return Optional.ofNullable(userInfo).map(UserInfo::getToken).map(token ->
                ok(new CommonAuthenticationResponse(token), authCookie(token, -1))
        ).orElseGet(() ->
                unauthorized("Cannot refresh token user info is either null or does not contain token")
        );
    }

    @ApiOperation("Refresh session token.")
    @RequestMapping(value = "/api/v1/auth/refresh", method = RequestMethod.POST)
    public ResponseEntity<JsonResult<?>> refresh(HttpServletRequest request) {

        String accessToken = request.getHeader(accessTokenResolver.getTokenHeaderName());
        UserInfo userInfo = authenticator.refreshToken(accessToken);
        return userService.findByUsername(userInfo.getUsername()).map(user ->
                ok(userInfo.getToken(), authCookie(userInfo.getToken(), -1))
        ).orElseGet(() ->
                unauthorized("User is not registered")
        );
    }

    @ApiOperation("Get current principal")
    @RequestMapping(value = "/api/v1/auth/me", method = GET)
    public JsonResult<UserInfoDTO> principal(Principal principal) {

        JsonResult<UserInfoDTO> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);

        userService
                .findByUsername(principal.getName())
                .ifPresent(user -> {
                    UserInfoDTO userInfoDTO = new UserInfoDTO();
                    userInfoDTO.setUsername(user.getUsername());
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
    public ResponseEntity<JsonResult<?>> logout(HttpServletRequest request) {

        try {
            String accessToken = request.getHeader(accessTokenResolver.getTokenHeaderName());
            if (StringUtils.isNotEmpty(accessToken)) {
                authenticator.invalidateToken(accessToken);
            }
            return ok(true, authCookie("", 0));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            JsonResult<Boolean> result = new JsonResult<>(JsonResult.ErrorCode.SYSTEM_ERROR, false);
            result.setErrorMessage(ex.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @ApiOperation(value = "Get professional types list")
    @RequestMapping(value = "/api/v1/user-management/professional-types", method = GET)
    public JsonResult<List<CommonProfessionalTypeDTO>> getProfessionalTypes() {

        requireNetworkMode(dataNodeService.getDataNodeMode());
        return integrationService.getProfessionalTypes();
    }

    @ApiOperation("Suggests country.")
    @RequestMapping(value = "/api/v1/user-management/countries/search", method = GET)
    public JsonResult<List<CommonCountryDTO>> suggestCountries(
            @RequestParam("query") String query,
            @RequestParam("limit") Integer limit,
            @RequestParam(value = "includeId", required = false) Long includeId

    ) {

        requireNetworkMode(dataNodeService.getDataNodeMode());
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

        requireNetworkMode(dataNodeService.getDataNodeMode());
        return integrationService.getStateProvinces(countryId, query, limit, includeId);
    }

    @ApiOperation("Register new user via form.")
    @RequestMapping(value = "/api/v1/auth/registration", method = RequestMethod.POST)
    public JsonResult<CommonUserDTO> register(@RequestBody CommonUserRegistrationDTO dto) {

        requireNetworkMode(dataNodeService.getDataNodeMode());
        return integrationService.getRegisterUser(dto);
    }

    @ApiOperation("Password restrictions")
    @RequestMapping(value = "/api/v1/auth/password-policies", method = GET)
    public ArachnePasswordInfoDTO getPasswordPolicies() throws URISyntaxException {

        requireNetworkMode(dataNodeService.getDataNodeMode());
        return integrationService.getPasswordInfo();
    }

    private <T> ResponseEntity<JsonResult<?>> ok(T body, String s) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, s)
                .body(new JsonResult<>(NO_ERROR, body));
    }

    private ResponseEntity<JsonResult<?>> unauthorized(String errorMessage) {
        JsonResult<String> result = new JsonResult<>(UNAUTHORIZED);
        result.setErrorMessage(errorMessage);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.SET_COOKIE, authCookie("", 0))
                .body(result);
    }

    private String authCookie(String token, int expiry) {
        return ResponseCookie.from(accessTokenResolver.getTokenHeaderName(), token)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite(SameSite.Strict.name())
                .maxAge(expiry)
                .build().toString();
    }


    @ApiOperation("Remind Password")
    @PostMapping(value = "/api/v1/auth/remind-password")
    public JsonResult<?> remindPassword(@Valid @RequestBody RemindPasswordDTO remindPasswordDTO) {

        requireNetworkMode(dataNodeService.getDataNodeMode());
        integrationService.remindPassword(remindPasswordDTO);
        return new JsonResult<>(NO_ERROR);
    }
}
