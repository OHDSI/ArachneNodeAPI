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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Nov 7, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.atlas;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.Objects;

public class AtlasAuthRequestInterceptor implements RequestInterceptor {

    private final AtlasLoginClient loginClient;
    private final AtlasAuthSchema authSchema;
    private final String username;
    private final String password;

    public AtlasAuthRequestInterceptor(AtlasLoginClient loginClient, AtlasAuthSchema authSchema, String username, String password) {

        this.loginClient = loginClient;
        this.authSchema = authSchema;
        this.username = username;
        this.password = password;
    }

    @Override
    public void apply(RequestTemplate template) {

        String token = authenticate();
        if (Objects.nonNull(token)) {
            template.header("Authorization", "Bearer " + token);
        }
    }

    private String authenticate(){
        String result = null;
        if (Objects.nonNull(authSchema)) {
            switch (authSchema) {
                case DATABASE:
                    result = loginClient.loginDatabase(username, password);
                    break;
                case LDAP:
                    result = loginClient.loginLdap(username, password);
                    break;
            }
        }
        return result;
    }
}
