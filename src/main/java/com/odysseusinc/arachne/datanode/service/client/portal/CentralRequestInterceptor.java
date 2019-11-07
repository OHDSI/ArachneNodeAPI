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
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.authenticator.service.authentication.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CentralRequestInterceptor implements feign.RequestInterceptor {

    @Value("${arachne.token.header}")
    private String authHeader;

    private ApplicationContext applicationContext;
    private UserService userService;
    private TokenService tokenService;

    @Autowired
    public CentralRequestInterceptor(ApplicationContext applicationContext) {

        this.applicationContext = applicationContext;
    }


    private String getToken() {

        final Object credentials = SecurityContextHolder.getContext().getAuthentication().getCredentials();
        if (credentials instanceof String && StringUtils.isNotEmpty(credentials.toString())) {
            String accessToken = (String)credentials;
            String centralToken = tokenService.resolveAdditionalInfo(accessToken, "token", String.class);
            return Objects.nonNull(centralToken) ? centralToken : credentials.toString();
        }
        return null;
    }

    @Override
    public void apply(RequestTemplate template) {

        final String token;
        init();
        token = getToken();
        if (!StringUtils.isEmpty(token)) {
            template.header(authHeader, token);
        }
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
