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

import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.AuthenticationService;
import com.odysseusinc.arachne.datanode.service.UserRegistrationStrategy;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.authenticator.service.AccessToken;
import org.ohdsi.authenticator.service.Authenticator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
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

    private UserRegistrationStrategy userRegisterStrategy;

    public AuthenticationServiceImpl(ApplicationContext applicationContext, Authenticator authenticator, UserRegistrationStrategy userRegisterStrategy) {

        this.applicationContext = applicationContext;
        this.authenticator = authenticator;
        this.userRegisterStrategy = userRegisterStrategy;
    }

    @Override
    public Authentication authenticate(AccessToken accessToken, HttpServletRequest httpRequest) {

        if (accessToken != null) {
            String username = authenticator.resolveUsername(accessToken);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                createUserByTokenIfNecessary(accessToken.getType(), username);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, accessToken, userDetails.getAuthorities());
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
    public void afterPropertiesSet() throws Exception {

        userDetailsService = applicationContext.getBean(UserDetailsService.class);
    }

    private void createUserByTokenIfNecessary(AccessToken.Type accessTokenType, String username) {
        if (accessTokenType != AccessToken.Type.IAP) {
            return;
        }
        if (StringUtils.isEmpty(username)) {
            throw new AuthException("Username cannot be empty.");
        }
        User user = getUserByUserEmail(username);
        userRegisterStrategy.registerUser(user);
    }

    private User getUserByUserEmail(String email) {
        String name = StringUtils.split(email, "@")[0];
        User user = new User();
        user.setFirstName(name);
        user.setLastName(StringUtils.EMPTY);
        user.setEmail(email);
        user.setUsername(email);
        return user;
    }

}
