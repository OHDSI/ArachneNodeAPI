/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
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

import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.UserService;
import feign.RequestTemplate;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CentralRequestInterceptor implements feign.RequestInterceptor {
    Logger log = LoggerFactory.getLogger(CentralRequestInterceptor.class);

    @Value("${arachne.token.header}")
    private String authHeader;

    private ApplicationContext applicationContext;
    private UserService userService;

    @Autowired
    public CentralRequestInterceptor(ApplicationContext applicationContext) {

        this.applicationContext = applicationContext;
    }


    private String getToken() throws PermissionDeniedException {

        final Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.User) {
            String userName = ((org.springframework.security.core.userdetails.User) principal).getUsername();
            final Optional<User> userOptional = userService.findByUsername(userName);
            return userOptional.map(User::getToken).orElse(null);
        }
        return null;
    }

    @Override
    public void apply(RequestTemplate template) {

        final String token;
        try {
            init();
            token = getToken();
            if (!StringUtils.isEmpty(token)) {
                template.header(authHeader, token);
            }
        } catch (PermissionDeniedException e) {
            log.error(e.getMessage());
        }
    }

    private void init(){

        if (Objects.isNull(this.userService)){
            this.userService = applicationContext.getBean(UserService.class);
        }
    }
}
