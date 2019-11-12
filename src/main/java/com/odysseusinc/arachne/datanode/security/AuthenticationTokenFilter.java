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

import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.ohdsi.authenticator.filter.JWTAuthenticationFilter;
import org.ohdsi.authenticator.service.authentication.Authenticator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

public class AuthenticationTokenFilter extends JWTAuthenticationFilter {

    @Value("${datanode.jwt.header}")
    private String tokenHeader;

    private final UserDetailsService userDetailsService;

    public AuthenticationTokenFilter(Authenticator authenticator,
                                     UserDetailsService userDetailsService) {

        super(authenticator);
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected String getToken(HttpServletRequest httpRequest) {

        return httpRequest.getHeader(this.tokenHeader);
    }

    @Override
    protected void onSuccessAuthentication(HttpServletRequest httpRequest, UserDetails userDetails, AbstractAuthenticationToken authentication) {

        if (Objects.nonNull(httpRequest)) {
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
        }
    }

    @Override
    protected UserDetails getUserDetails(String username) {

        return userDetailsService.loadUserByUsername(username);
    }
}