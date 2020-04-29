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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Nov 7, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.atlas;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.odysseusinc.arachne.datanode.Constants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.ohdsi.authenticator.exception.AuthenticationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Objects;

public class AtlasAuthRequestInterceptor implements RequestInterceptor {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private final AtlasLoginClient loginClient;
    private final AtlasAuthSchema authSchema;
    private final String username;
    private final String password;
    private final String keyfile;
    private final String serviceId;

    public AtlasAuthRequestInterceptor(AtlasLoginClient loginClient, AtlasAuthentication auth) {

        this.loginClient = loginClient;
        this.authSchema = auth.getSchema();
        this.username = auth.getUsername();
        this.password = auth.getPassword();
        this.keyfile = auth.getKeyfile();
        this.serviceId = auth.getServiceId();
    }

    @Override
    public void apply(RequestTemplate template) {

        if (!Objects.equals(AtlasAuthSchema.NONE, authSchema)) {
            String token = null;
            try {
                token = authenticate();
            } catch (UnsupportedEncodingException ignored) {
            }
            if (Objects.nonNull(token)) {
                template.header(AUTHORIZATION_HEADER, BEARER_PREFIX + token);
            }
        }
    }

    private String authenticate() throws UnsupportedEncodingException {
        String result = null;

        if (Objects.nonNull(authSchema)) {
            switch (authSchema) {
                case DATABASE:
                    result = loginClient.loginDatabase(username, password);
                    break;
                case LDAP:
                    result = loginClient.loginLdap(username, password);
                    break;
                case ACCESS_TOKEN:
                    result = generateJWTToken(serviceId, keyfile);
                    break;
            }
        }
        return result;
    }

    /* serviceId currently is not used to generate JWT but present for future releases of client library */
    private String generateJWTToken(String serviceId, String keyfile) {

        try(InputStream in = new ByteArrayInputStream(keyfile.getBytes())) {
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(in)
                    .createScoped(Collections.singletonList(Constants.GOOGLE_AUTH_SCOPE));
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            return token.getTokenValue();
        } catch (IOException e) {
            throw new AuthenticationException(e.getMessage());
        }
    }
}
