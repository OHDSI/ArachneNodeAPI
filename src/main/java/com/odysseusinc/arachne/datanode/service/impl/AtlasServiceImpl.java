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

import static com.odysseusinc.arachne.datanode.util.DataSourceUtils.isNotDummyPassword;

import com.odysseusinc.arachne.commons.api.v1.dto.AtlasShortDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.exception.ServiceNotAvailableException;
import com.odysseusinc.arachne.datanode.service.client.ArachneHttpClientBuilder;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.repository.AtlasRepository;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthRequestInterceptor;


import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthSchema;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasLoginClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.TokenDecoder;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import feign.Client;
import feign.Feign;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtlasServiceImpl implements AtlasService {
    private Logger log = LoggerFactory.getLogger(AtlasServiceImpl.class);

    private final GenericConversionService conversionService;
    private final ArachneHttpClientBuilder arachneHttpClientBuilder;
    private final AtlasRepository atlasRepository;
    private final CentralSystemClient centralSystemClient;
    private HttpHeaders headers;

    private Map<Atlas, AtlasClient> atlasClientPool = new HashMap<>();

    @Autowired
    public AtlasServiceImpl(GenericConversionService genericConversionService,
                            AtlasRepository atlasRepository,
                            CentralSystemClient centralSystemClient,
                            ArachneHttpClientBuilder arachneHttpClientBuilder) {

        this.conversionService = genericConversionService;
        this.atlasRepository = atlasRepository;
        this.centralSystemClient = centralSystemClient;
        this.arachneHttpClientBuilder = arachneHttpClientBuilder;
    }

    @Override
    public List<Atlas> findAll() {

        return atlasRepository.findAll();
    }

    @Override
    public Page<Atlas> findAll(Pageable pageable) {

        return atlasRepository.findAll(pageable);
    }

    @Override
    public Atlas getById(Long id) {

        return atlasRepository.findOne(id);
    }

    @Override
    public String checkConnection(Atlas atlas) {

        headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE));

        String atlasVersion = checkVersion(atlas);

        if (!Objects.equals(AtlasAuthSchema.NONE, atlas.getAuthType())) {
            String atlasToken = authToAtlas(atlas.getUrl(), atlas.getAuthType(), atlas.getUsername(), atlas.getPassword());
            headers.add(AtlasAuthRequestInterceptor.AUTHORIZATION_HEADER, AtlasAuthRequestInterceptor.BEARER_PREFIX + atlasToken);
        }

        getCohortDefinitionCount(atlas);

        return Optional.ofNullable(atlasVersion).orElseThrow(
                () -> new ServiceNotAvailableException("Atlas version is null"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Atlas updateVersion(Long atlasId, String version) {

        Atlas atlas = atlasRepository.findOne(atlasId);
        atlas.setVersion(version);

        AtlasShortDTO updatedDTO = syncWithCentral(atlas);

        atlas.setCentralId(updatedDTO.getCentralId());

        return save(atlas);
    }

    @Override
    public Atlas save(Atlas atlas) {

        return atlasRepository.saveAndFlush(atlas);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Atlas update(Long atlasId, Atlas atlas) {

        Atlas existing = atlasRepository.findOne(atlasId);
        boolean shouldSyncWithCentral = false;

        if (atlas.getName() != null) {
            existing.setName(atlas.getName());
            shouldSyncWithCentral = true;
        }

        if (atlas.getUrl() != null) {
            existing.setUrl(atlas.getUrl());
        }

        if (atlas.getAuthType() != null) {
            existing.setAuthType(atlas.getAuthType());
        }

        if (atlas.getUsername() != null) {
            existing.setUsername(atlas.getUsername());
        }

        if (isNotDummyPassword(atlas.getPassword())) {
            existing.setPassword(atlas.getPassword());
        }

        Atlas updated = atlasRepository.saveAndFlush(existing);

        if (shouldSyncWithCentral) {
            syncWithCentral(updated);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long atlasId) {

        Atlas atlas = atlasRepository.findOne(atlasId);
        atlasRepository.delete(atlas.getId());
        centralSystemClient.deleteAtlas(atlas.getCentralId());
    }

    @Override
    public <R extends BaseAtlasEntity> List<R> execute(List<Atlas> atlasList, Function<? super AtlasClient, ? extends List<R>> sendAtlasRequest) {

        return atlasList.parallelStream()
                .map(atlas -> {
                    try {
                        AtlasClient client = getOrCreate(atlas);
                        List<R> list = sendAtlasRequest.apply(client);
                        list.forEach(entry -> entry.setOrigin(atlas));
                        return list;
                    } catch (Exception ex) {
                        log.error("Cannot fetch data from Atlas with id = " + atlas.getId(), ex);
                        return new ArrayList<R>();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public <R> R execute(Atlas atlas, Function<? super AtlasClient, R> sendAtlasRequest) {

        AtlasClient client = getOrCreate(atlas);
        return sendAtlasRequest.apply(client);
    }

    private AtlasShortDTO syncWithCentral(Atlas atlas) {

        AtlasShortDTO atlasShortDTO = conversionService.convert(atlas, AtlasShortDTO.class);
        return centralSystemClient.updateAtlasInfo(atlasShortDTO);
    }

    private AtlasClient getOrCreate(Atlas atlas) {

        return atlasClientPool.computeIfAbsent(atlas, this::buildAtlasClient);
    }

    private String checkVersion(Atlas atlas) {

        AtlasClient.Info info;
        try {
            info = getOrCreate(atlas).getInfo();
        } catch (Exception e) {
            throw new ServiceNotAvailableException("Incorrect Atlas address");
        }
        return info != null ? info.version : null;
    }

    private String authToAtlas(String url, AtlasAuthSchema authSchema, String login, String password) {

        String atlasToken = null;
        AtlasLoginClient atlasLoginClient = buildAtlasLoginClient(url, arachneHttpClientBuilder.build());
        if (Objects.nonNull(authSchema)) {
            try {
                switch (authSchema) {
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

    private AtlasClient buildAtlasClient(Atlas atlas) {

        Client httpClient  = arachneHttpClientBuilder.build();
        return Feign.builder()
                .client(httpClient)
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(AtlasClient.class))
                .logLevel(feign.Logger.Level.FULL)
                .requestInterceptor(new AtlasAuthRequestInterceptor(buildAtlasLoginClient(atlas.getUrl(), httpClient), atlas.getAuthType(), atlas.getUsername(), atlas.getPassword()))
                .target(AtlasClient.class, atlas.getUrl());
    }

    private static AtlasLoginClient buildAtlasLoginClient(String url, Client httpClient) {

        return Feign.builder()
                .client(httpClient)
                .encoder(new FormEncoder(new JacksonEncoder()))
                .decoder(new TokenDecoder())
                .logger(new Slf4jLogger(AtlasLoginClient.class))
                .target(AtlasLoginClient.class, url);
    }

    private void getCohortDefinitionCount(Atlas atlas) {

        try {
            getOrCreate(atlas).getCohortDefinitions();
        } catch (Exception e) {
            throw new ServiceNotAvailableException("Failed to get Cohort Definitions");
        }
    }
}
