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

package com.odysseusinc.arachne.datanode.security;

import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.service.AuthenticationService;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.authenticator.service.authentication.AccessTokenResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.GenericFilterBean;

public class AuthenticationTokenFilter extends GenericFilterBean {

    Logger log = LoggerFactory.getLogger(AuthenticationTokenFilter.class);

    @Autowired
    private AccessTokenResolver accessTokenResolver;

    @Autowired
    private AuthenticationService authenticationService;

    @Value("${security.method}")
    private String authMethod;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {


        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String accessToken = httpRequest.getHeader(accessTokenResolver.getTokenHeaderName());
        if (StringUtils.isNotEmpty(accessToken)) {
            try {
                authenticationService.authenticate(accessToken, httpRequest);
            } catch (AuthenticationException | AuthException | org.ohdsi.authenticator.exception.AuthenticationException ex) {
                logAuthenticationException(httpRequest, ex);
            }
        }
        chain.doFilter(request, response);
    }

    private void logAuthenticationException(HttpServletRequest httpRequest, RuntimeException ex) {

        if (HttpMethod.OPTIONS.matches(httpRequest.getMethod())) {
            return;
        }
        log.debug("Authentication failed", ex);
    }

}
