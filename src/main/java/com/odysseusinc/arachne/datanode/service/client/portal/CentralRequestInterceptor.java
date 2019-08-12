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
 * Created: August 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.portal;

import com.odysseusinc.arachne.datanode.service.UserService;
import feign.RequestTemplate;
import java.util.Objects;
import org.jasypt.util.text.StrongTextEncryptor;
import org.jasypt.util.text.TextEncryptor;
import org.ohdsi.authenticator.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CentralRequestInterceptor implements feign.RequestInterceptor {
    Logger log = LoggerFactory.getLogger(CentralRequestInterceptor.class);

    @Value("${arachne.token.header}")
    private String authHeader;

    @Value("${datanode.arachneCentral.nodeAuthHeader}")
    private String nodeAuthHeader;

    @Value("${datanode.arachneCentral.impersonateHeader}")
    private String impersonateHeader;

    private ApplicationContext applicationContext;
    private UserService userService;
    private TokenService tokenService;

    @Autowired
    public CentralRequestInterceptor(ApplicationContext applicationContext) {

        this.applicationContext = applicationContext;
    }


    private void setupAuth(RequestTemplate template) {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            final Object credentials = authentication.getCredentials();
            if (credentials instanceof String) {
                String userToken = tokenService.resolveAdditionalInfo(credentials.toString(), "token", String.class);
                String token = Objects.nonNull(userToken) ? userToken : credentials.toString();;
                template.header(authHeader, token);
            }
        } else if (authentication instanceof RunAsUserToken) {
            final Object systemToken = authentication.getCredentials();
            final Object username = authentication.getPrincipal();
            if (systemToken instanceof String && username instanceof String) {
                TextEncryptor encryptor = getEncryptor(systemToken.toString());
                template.header(nodeAuthHeader, systemToken.toString());
                template.header(impersonateHeader, encryptor.encrypt(username.toString()));
            }
        }
    }

    private TextEncryptor getEncryptor(String systemToken) {

        StrongTextEncryptor encryptor = new StrongTextEncryptor();
        encryptor.setPassword(systemToken);
        return encryptor;
    }

    @Override
    public void apply(RequestTemplate template) {

        final String token;
        init();
        setupAuth(template);
    }

    private void init(){

        if (Objects.isNull(this.userService)) {
            this.userService = applicationContext.getBean(UserService.class);
        }
        if (Objects.isNull(this.tokenService)) {
            this.tokenService = applicationContext.getBean(TokenService.class);
        }
    }
}
