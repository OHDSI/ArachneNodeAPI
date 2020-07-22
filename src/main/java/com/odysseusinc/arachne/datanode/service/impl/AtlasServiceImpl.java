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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odysseusinc.arachne.commons.api.v1.dto.AtlasShortDTO;
import com.odysseusinc.arachne.commons.utils.CommonFileUtils;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.exception.ServiceNotAvailableException;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.datanode.repository.AtlasRepository;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.client.atlas.*;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.events.atlas.AtlasDeletedEvent;
import com.odysseusinc.arachne.datanode.service.events.atlas.AtlasUpdatedEvent;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.hydra.Hydra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.odysseusinc.arachne.datanode.util.DataSourceUtils.isNotDummyPassword;
import static com.odysseusinc.arachne.datanode.util.DataSourceUtils.isNotDummyValue;

@Service
public class AtlasServiceImpl implements AtlasService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtlasServiceImpl.class);

    private final AtlasClientServiceImpl atlasClientService;
    private final GenericConversionService conversionService;
    private final AtlasRepository atlasRepository;
    private final CentralSystemClient centralSystemClient;
    private final DataNodeService dataNodeService;
    private final ApplicationEventPublisher eventPublisher;

    private Map<Atlas, ? extends AtlasClient> atlasClientPool = new ConcurrentHashMap<>();

    @Autowired
    public AtlasServiceImpl(AtlasClientServiceImpl atlasClientService,
                            GenericConversionService genericConversionService,
                            AtlasRepository atlasRepository,
                            CentralSystemClient centralSystemClient,
                            DataNodeService dataNodeService,
                            ApplicationEventPublisher eventPublisher) {

        this.atlasClientService = atlasClientService;

        this.conversionService = genericConversionService;
        this.atlasRepository = atlasRepository;
        this.centralSystemClient = centralSystemClient;
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

        String atlasVersion = checkVersion(atlas);
        if (atlasVersion == null) {
            throw new ServiceNotAvailableException("Atlas version is null");
        }

        atlasPingWithAuthenticationRequest(atlas);
        return atlasVersion;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Atlas updateVersion(Long atlasId, String version) {

        Atlas atlas = atlasRepository.findOne(atlasId);
        atlas.setVersion(version);

        AtlasShortDTO updatedDTO = updateOnCentral(atlas);

        atlas.setCentralId(updatedDTO.getCentralId());

        Atlas updated = save(atlas);
        atlasClientPool.replace(updated, atlasClientService.buildAtlasClient(updated));
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

        if (Objects.nonNull(atlas.getServiceId())) {
            existing.setServiceId(atlas.getServiceId());
        }

        if (isNotDummyValue(atlas.getKeyfile(), Constants.DUMMY_KEYFILE)) {
            existing.setKeyfile(atlas.getKeyfile());
        }

        Atlas updated = atlasRepository.saveAndFlush(existing);

        if (shouldSyncWithCentral) {
            eventPublisher.publishEvent(new AtlasUpdatedEvent(this, updated));
        }

        atlasClientPool.replace(updated, atlasClientService.buildAtlasClient(updated));
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
                        LOGGER.error("Cannot fetch data from Atlas with id = " + atlas.getId(), ex);
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

        AtlasInfoClient infoClient = atlasClientService.buildAtlasInfoClient(atlas);
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
            LOGGER.warn("Failed to sync Atlas {} with Central, {}", atlas.getName(), e.getMessage());
        }
        return atlasShortDTO;
    }

    private <T extends AtlasClient> T getOrCreate(Atlas atlas) {

        return (T)atlasClientPool.computeIfAbsent(atlas, this.atlasClientService::buildAtlasClient);
    }

    private String checkVersion(Atlas atlas) {

        AtlasClient.Info info;
        try {
            info = atlasClientService.buildAtlasInfoClient(atlas).getInfo();
        } catch (Exception e) {
            throw new ServiceNotAvailableException("Incorrect Atlas address");
        }
        return info != null ? info.version : null;
    }

    private void atlasPingWithAuthenticationRequest(Atlas atlas) {

        try {
            getOrCreate(atlas).getCohortDefinitions();
        } catch (Exception e) {
            throw new ServiceNotAvailableException("Failed to get Cohort Definitions");
        }
    }
}
