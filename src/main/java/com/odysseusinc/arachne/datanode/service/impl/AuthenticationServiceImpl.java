/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Jul 29, 2019
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.service.AuthenticationService;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.ohdsi.authenticator.service.Authenticator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

public class AuthenticationServiceImpl implements AuthenticationService, InitializingBean {

    private final Authenticator authenticator;

    private final ApplicationContext applicationContext;

    private UserDetailsService userDetailsService;

    public AuthenticationServiceImpl(ApplicationContext applicationContext, Authenticator authenticator) {

        this.applicationContext = applicationContext;
        this.authenticator = authenticator;
    }

    @Override
    public Authentication authenticate(String authToken, HttpServletRequest httpRequest) {

        if (authToken != null) {
            String username = authenticator.resolveUsername(authToken);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, authToken, userDetails.getAuthorities());
                if (Objects.nonNull(httpRequest)) {
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
                }
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return authentication;
            }
        }
        return null;
    }

    @Override
    public Authentication impersonate(String systemToken, String username) {

        if (Objects.nonNull(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            RunAsUserToken authentication = new RunAsUserToken(systemToken, username, systemToken,
                    userDetails.getAuthorities(), UsernamePasswordAuthenticationToken.class);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return authentication;
        }
        return null;
    }

    @Override
    public String getCurrentUserName() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                return authentication.getName();
            }
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        userDetailsService = applicationContext.getBean(UserDetailsService.class);
    }
}
