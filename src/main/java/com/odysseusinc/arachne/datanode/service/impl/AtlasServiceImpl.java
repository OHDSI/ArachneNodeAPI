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
 * Created: December 05, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import static com.odysseusinc.arachne.datanode.Constants.Atlas.ATLAS_2_7_VERSION;
import static com.odysseusinc.arachne.datanode.util.DataSourceUtils.isNotDummyPassword;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odysseusinc.arachne.commons.api.v1.dto.AtlasShortDTO;
import com.odysseusinc.arachne.commons.utils.CommonFileUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.dto.serialize.PageModule;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.ServiceNotAvailableException;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.datanode.repository.AtlasRepository;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.client.ArachneHttpClientBuilder;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthRequestInterceptor;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthSchema;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_5;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasInfoClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasLoginClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.TokenDecoder;
import com.odysseusinc.arachne.datanode.service.client.decoders.ByteArrayDecoder;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.events.atlas.AtlasDeletedEvent;
import com.odysseusinc.arachne.datanode.service.events.atlas.AtlasUpdatedEvent;
import feign.Client;
import feign.Feign;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.hydra.Hydra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtlasServiceImpl implements AtlasService {

    private static final String ATLAS_LOWER_VERSION = "2.2.0";
    private Logger log = LoggerFactory.getLogger(AtlasServiceImpl.class);

    private final GenericConversionService conversionService;
    private final ArachneHttpClientBuilder arachneHttpClientBuilder;
    private final AtlasRepository atlasRepository;
    private final CentralSystemClient centralSystemClient;
    private final DataNodeService dataNodeService;
    private final ApplicationEventPublisher eventPublisher;
    private HttpHeaders headers;

    private Map<Atlas, ? extends AtlasClient> atlasClientPool = new ConcurrentHashMap<>();
    private static final List<Module> MODULES = Collections.singletonList(new PageModule());

    @Autowired
    public AtlasServiceImpl(GenericConversionService genericConversionService,
                            AtlasRepository atlasRepository,
                            CentralSystemClient centralSystemClient,
                            ArachneHttpClientBuilder arachneHttpClientBuilder,
                            DataNodeService dataNodeService,
                            ApplicationEventPublisher eventPublisher) {

        this.conversionService = genericConversionService;
        this.atlasRepository = atlasRepository;
        this.centralSystemClient = centralSystemClient;
        this.arachneHttpClientBuilder = arachneHttpClientBuilder;
        this.dataNodeService = dataNodeService;
        this.eventPublisher = eventPublisher;
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

        AtlasShortDTO updatedDTO = updateOnCentral(atlas);

        atlas.setCentralId(updatedDTO.getCentralId());

        Atlas updated = save(atlas);
        atlasClientPool.replace(updated, buildAtlasClient(updated));
        return updated;
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
            eventPublisher.publishEvent(new AtlasUpdatedEvent(this, updated));
        }

        atlasClientPool.replace(updated, buildAtlasClient(updated));
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long atlasId) {

        Atlas atlas = atlasRepository.findOne(atlasId);
        atlasRepository.delete(atlas.getId());

        eventPublisher.publishEvent(new AtlasDeletedEvent(this, atlas));

        atlasClientPool.remove(atlas);
    }

    @Override
    public void deleteFromCentral(Atlas atlas) {

        if (Objects.nonNull(atlas) && Objects.nonNull(atlas.getId())) {
            centralSystemClient.deleteAtlas(atlas.getCentralId());
        }
    }

    @Override
    public <C extends AtlasClient, R extends BaseAtlasEntity> List<R> execute(List<Atlas> atlasList, Function<C, ? extends List<R>> sendAtlasRequest) {

        return atlasList.parallelStream()
                .map(atlas -> {
                    try {
                        C client = getOrCreate(atlas);
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
    public <C extends AtlasClient, R> R execute(Atlas atlas, Function<C, R> sendAtlasRequest) {

        C client = getOrCreate(atlas);
        return sendAtlasRequest.apply(client);
    }

    @Override
    public <R> R executeInfo(Atlas atlas, Function<AtlasInfoClient, R> sendAtlasRequest) {

        AtlasInfoClient infoClient = buildAtlasInfoClient(atlas);
        return sendAtlasRequest.apply(infoClient);
    }

    @Override
    public byte[] hydrateAnalysis(JsonNode analysis, String packageName) throws IOException {

        return hydrateAnalysis(analysis, packageName, null);
    }

    @Override
    public byte[] hydrateAnalysis(JsonNode analysis, String packageName, String skeletonResource) throws IOException {

        ((ObjectNode)analysis).put("packageName", packageName);
        Hydra hydra = new Hydra(analysis.toString());
        if (StringUtils.isNotBlank(skeletonResource)) {
            File skeletonFile = CommonFileUtils.copyResourceToTempFile(skeletonResource, "skeleton-", ".zip");
            hydra.setExternalSkeletonFileName(skeletonFile.getAbsolutePath());
        }
        byte[] data;
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            hydra.hydrate(out);
            data = out.toByteArray();
        }
        return data;
    }

    @Override
    public AtlasShortDTO updateOnCentral(Atlas atlas) {

        AtlasShortDTO atlasShortDTO = conversionService.convert(atlas, AtlasShortDTO.class);
        try {
            if (Objects.equals(dataNodeService.getDataNodeMode(), FunctionalMode.NETWORK)) {
                atlasShortDTO = centralSystemClient.updateAtlasInfo(atlasShortDTO);
            }
        } catch (Exception e) {
            log.warn("Failed to sync Atlas {} with Central, {}", atlas.getName(), e.getMessage());
        }
        return atlasShortDTO;
    }

    private <T extends AtlasClient> T getOrCreate(Atlas atlas) {

        return (T)atlasClientPool.computeIfAbsent(atlas, this::buildAtlasClient);
    }

    private String checkVersion(Atlas atlas) {

        AtlasClient.Info info;
        try {
            info = buildAtlasInfoClient(atlas).getInfo();
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

    private <T extends AtlasClient> T buildAtlasClient(Atlas atlas) {

        Client httpClient  = arachneHttpClientBuilder.build();
        return Feign.builder()
                .client(httpClient)
                .encoder(new JacksonEncoder())
                .decoder(new ByteArrayDecoder(new JacksonDecoder(MODULES)))
                .logger(new Slf4jLogger(AtlasClient.class))
                .logLevel(feign.Logger.Level.FULL)
                .requestInterceptor(new AtlasAuthRequestInterceptor(buildAtlasLoginClient(atlas.getUrl(), httpClient), atlas.getAuthType(), atlas.getUsername(), atlas.getPassword()))
                .target(getAtlasClientClass(atlas), atlas.getUrl());
    }

    private <T extends AtlasClient> Class<T> getAtlasClientClass(Atlas atlas) {

        String version = Objects.nonNull(atlas) && Objects.nonNull(atlas.getVersion()) ? atlas.getVersion() : ATLAS_LOWER_VERSION;
        return ATLAS_2_7_VERSION.isLesserOrEqualsThan(version) ? (Class<T>) AtlasClient2_7.class : (Class<T>) AtlasClient2_5.class;
    }

    private static AtlasLoginClient buildAtlasLoginClient(String url, Client httpClient) {

        return Feign.builder()
                .client(httpClient)
                .encoder(new FormEncoder(new JacksonEncoder()))
                .decoder(new TokenDecoder())
                .logger(new Slf4jLogger(AtlasLoginClient.class))
                .target(AtlasLoginClient.class, url);
    }

    private AtlasInfoClient buildAtlasInfoClient(Atlas atlas) {

        Client httpClient = arachneHttpClientBuilder.build();
        return Feign.builder()
                .client(httpClient)
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(AtlasInfoClient.class))
                .logLevel(feign.Logger.Level.BASIC)
                .target(AtlasInfoClient.class, atlas.getUrl());
    }

    private void getCohortDefinitionCount(Atlas atlas) {

        try {
            getOrCreate(atlas).getCohortDefinitions();
        } catch (Exception e) {
            throw new ServiceNotAvailableException("Failed to get Cohort Definitions");
        }
    }
}
