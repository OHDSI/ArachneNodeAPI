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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: December 05, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.exception.ServiceNotAvailableException;
import com.odysseusinc.arachne.datanode.service.client.ArachneHttpClientBuilder;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthRequestInterceptor;


import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthSchema;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClientConfig;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasLoginClient;
import com.odysseusinc.arachne.system.settings.api.v1.dto.SystemSettingDTO;
import com.odysseusinc.arachne.system.settings.api.v1.dto.SystemSettingsGroupDTO;
import com.odysseusinc.arachne.system.settings.repository.SystemSettingsGroupRepository;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AtlasServiceImpl implements AtlasService {
    private final RestTemplate atlasRestTemplate;
    private final GenericConversionService conversionService;
    private final SystemSettingsGroupRepository systemSettingsGroupRepository;
    private final ArachneHttpClientBuilder arachneHttpClientBuilder;
    private HttpHeaders headers;

    private static final String ATLAS_SETTINGS_GROUP_NAME = "atlas";
    private static final String ATLAS_HOST = "atlas.host";
    private static final String ATLAS_PORT = "atlas.port";
    private static final String ATLAS_CONTEXT = "atlas.urlContext";
    private static final String ATLAS_AUTH_SCHEMA = "atlas.auth.schema";
    private static final String ATLAS_AUTH_USERNAME = "atlas.auth.username";
    private static final String ATLAS_AUTH_PASSWORD = "atlas.auth.password";

    @Autowired
    public AtlasServiceImpl(GenericConversionService genericConversionService,
                            SystemSettingsGroupRepository settingsGroupRepository,
                            @Qualifier("atlasRestTemplate") RestTemplate atlasRestTemplate,
                            ArachneHttpClientBuilder arachneHttpClientBuilder) {

        this.conversionService = genericConversionService;
        this.systemSettingsGroupRepository = settingsGroupRepository;
        this.atlasRestTemplate = atlasRestTemplate;
        this.arachneHttpClientBuilder = arachneHttpClientBuilder;
    }


    @Override
    public String checkConnection() {

        SystemSettingsGroupDTO systemSettingsGroup =
                conversionService.convert(systemSettingsGroupRepository.findByName(ATLAS_SETTINGS_GROUP_NAME), SystemSettingsGroupDTO.class);
        Map<String, String> settings = systemSettingsGroup.getFieldList().stream().collect(Collectors.toMap(SystemSettingDTO::getName,
                SystemSettingDTO::getValue));
        String url = AtlasClientConfig.getAtlasUrl(settings.get(ATLAS_HOST),
                Integer.valueOf(settings.get(ATLAS_PORT)), settings.get(ATLAS_CONTEXT));
        headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE));

        String atlasVersion = checkVersion(url);
        String atlasToken = authToAtlas(url, settings.get(ATLAS_AUTH_SCHEMA),
                settings.get(ATLAS_AUTH_USERNAME), settings.get(ATLAS_AUTH_PASSWORD));
        headers.add(AtlasAuthRequestInterceptor.AUTHORIZATION_HEADER, AtlasAuthRequestInterceptor.BEARER_PREFIX + atlasToken);

        getCohortDefinitionCount(url);

        return Optional.ofNullable(atlasVersion).orElseThrow(
                () -> new ServiceNotAvailableException("Atlas version is null"));

    }

    private String checkVersion(String url) {

        AtlasClient.Info info;
        try {
            info = atlasRestTemplate.getForObject(
                    new URI(url + Constants.Atlas.INFO), AtlasClient.Info.class);
        } catch (Exception e) {
            throw new ServiceNotAvailableException("Incorrect Atlas address");
        }
        return info != null ? info.version : null;
    }

    private String authToAtlas(String url, String authSchema, String login, String password) {

        String atlasToken = null;
        AtlasLoginClient atlasLoginClient = AtlasClientConfig.buildAtlasLoginClient(url, arachneHttpClientBuilder.build());
        if (!StringUtils.isBlank(authSchema)) {
            try {
                switch (AtlasAuthSchema.valueOf(authSchema)) {
                    case DATABASE:
                        atlasToken = atlasLoginClient.loginDatabase(login, password);
                        break;
                    case LDAP:
                        atlasToken = atlasLoginClient.loginLdap(login, password);
                        break;
                    case NONE:
                        throw new AuthException("Atlas auth error");
                }
            } catch (IllegalArgumentException e) {
                throw new AuthException("Unsupported authentication type");
            } catch (Exception e) {
                throw new AuthException("Atlas auth error");
            }
        }
        return Optional.ofNullable(atlasToken).orElseThrow(() -> new AuthException("Atlas token is null"));
    }

    private void getCohortDefinitionCount(String url) {

        try {
            HttpEntity request = new HttpEntity(headers);
            atlasRestTemplate.exchange(
                    new URI(url + Constants.Atlas.COHORT_DEFINITION),
                    HttpMethod.GET, request,
                    new ParameterizedTypeReference<List<CohortDefinition>>() {
                    });
        } catch (Exception e) {
            throw new ServiceNotAvailableException("Failed to get Cohort Definitions");
        }
    }
}
