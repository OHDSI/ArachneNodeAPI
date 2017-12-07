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
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthRequestInterceptor;


import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthSchema;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.system.settings.api.v1.dto.SystemSettingDTO;
import com.odysseusinc.arachne.system.settings.api.v1.dto.SystemSettingsGroupDTO;
import com.odysseusinc.arachne.system.settings.repository.SystemSettingsGroupRepository;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AtlasServiceImpl implements AtlasService {
    private final RestTemplate atlasRestTemplate;
    private final GenericConversionService conversionService;
    private final SystemSettingsGroupRepository systemSettingsGroupRepository;
    private HttpHeaders headers;

    @Autowired
    public AtlasServiceImpl(GenericConversionService genericConversionService,
                            SystemSettingsGroupRepository settingsGroupRepository,
                            @Qualifier("atlasRestTemplate") RestTemplate atlasRestTemplate) {

        this.conversionService = genericConversionService;
        this.systemSettingsGroupRepository = settingsGroupRepository;
        this.atlasRestTemplate = atlasRestTemplate;
    }


    @Override
    public String checkConnection() {

        SystemSettingsGroupDTO systemSettingsGroup =
                conversionService.convert(systemSettingsGroupRepository.findByName("atlas"), SystemSettingsGroupDTO.class);
        Map<String, String> settings = systemSettingsGroup.getFieldList().stream().collect(Collectors.toMap(SystemSettingDTO::getName,
                SystemSettingDTO::getValue));
        String url = settings.get("atlas.host") + ":" + settings.get("atlas.port")
                + (StringUtils.isBlank(settings.get("atlas.urlContext")) ? ""
                : "/" + settings.get("atlas.urlContext"));
        headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE));

        String atlasVersion = checkVersion(url);
        String atlasToken = authToAtlas(url, settings.get("atlas.auth.schema"),
                settings.get("atlas.auth.username"), settings.get("atlas.auth.password"));
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
        if (!StringUtils.isBlank(authSchema)) {
            HttpEntity request = new HttpEntity<>("login=" +
                    login + "&password=" + password, headers);
            ResponseEntity<String> response;
            try {
                switch (AtlasAuthSchema.valueOf(authSchema)) {
                    case DATABASE:
                        response = atlasRestTemplate.postForEntity(
                                new URI(url + Constants.Atlas.LOGIN_DB), request, String.class);
                        atlasToken = response.getHeaders().get(AtlasAuthRequestInterceptor.BEARER_PREFIX.trim()).get(0);
                        break;
                    case LDAP:
                        response = atlasRestTemplate.postForEntity(
                                new URI(url + Constants.Atlas.LOGIN_LDAP), request, String.class);
                        atlasToken = response.getHeaders().get(AtlasAuthRequestInterceptor.BEARER_PREFIX.trim()).get(0);
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
