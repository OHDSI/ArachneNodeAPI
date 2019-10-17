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
 * Created: December 19, 2016
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.NO_ERROR;
import static com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode.NETWORK;
import static com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode.STANDALONE;
import static com.odysseusinc.arachne.datanode.util.DataSourceUtils.isNotDummyPassword;

import com.google.common.base.Preconditions;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonHealthStatus;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonModelType;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.datanode.dto.converters.DataSourceDTOToDataSourceConverter;
import com.odysseusinc.arachne.datanode.dto.converters.DataSourceToCommonDataSourceDTOConverter;
import com.odysseusinc.arachne.datanode.dto.converters.UserDTOToUserConverter;
import com.odysseusinc.arachne.datanode.dto.converters.UserToUserDTOConverter;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.exception.ValidationException;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.datanode.model.datasource.AutoDetectedFields;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.repository.DataSourceRepository;
import com.odysseusinc.arachne.datanode.service.BaseCentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.datanode.service.events.datasource.DataSourceCreatedEvent;
import com.odysseusinc.arachne.datanode.service.events.datasource.DataSourceUpdatedEvent;
import com.odysseusinc.arachne.datanode.util.DataNodeUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DataSourceServiceImpl implements DataSourceService {

    private static final String DATANODE_IS_NOT_EXIST_EXCEPTION = "DataNode entry is not exist, create it before";

    private final DataSourceRepository dataSourceRepository;
    private final DataNodeService dataNodeService;
    private final Map<String, String> dsSortPath = new HashMap<>();
    protected final GenericConversionService conversionService;
    protected final ApplicationEventPublisher eventPublisher;
    protected final BaseCentralIntegrationService<DataSource, CommonDataSourceDTO> integrationService;
    protected final CentralClient centralClient;

    @Autowired
    public DataSourceServiceImpl(DataSourceRepository dataSourceRepository,
                                 DataNodeService dataNodeService,
                                 BaseCentralIntegrationService<DataSource, CommonDataSourceDTO> integrationService,
                                 GenericConversionService conversionService,
                                 ApplicationEventPublisher eventPublisher,
                                 CentralClient centralClient) {

        this.dataSourceRepository = dataSourceRepository;
        this.dataNodeService = dataNodeService;
        this.integrationService = integrationService;
        this.conversionService = conversionService;
        this.eventPublisher = eventPublisher;
        this.centralClient = centralClient;
    }

    @PostConstruct
    private void init() {

        this.dsSortPath.put("name", "name");
        this.dsSortPath.put("dbmsType", "type");
        this.dsSortPath.put("connectionString", "connectionString");
        this.dsSortPath.put("cdmSchema", "cdmSchema");
        this.dsSortPath.put("modelType", "modelType");
        this.dsSortPath.put("isRegistered", "registred");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DataSource create(User owner, DataSource dataSource) throws NotExistException {

        DataNode currentDataNode = dataNodeService.findCurrentDataNodeOrCreate(owner);
        dataSource.setDataNode(currentDataNode);

        checkNotNull(dataSource, "given datasource is null");
        checkNotNull(owner, "given owner is null");
        DataSource created = dataSourceRepository.save(dataSource);
        eventPublisher.publishEvent(new DataSourceCreatedEvent(this, owner, dataSource));
        return created;
    }

    @Override
    public void createOnCentral(User owner, DataSource dataSource) {

        AutoDetectedFields autoDetectedFields = autoDetectFields(dataSource);
        if (Objects.equals(NETWORK, dataNodeService.getDataNodeMode())) {
            CommonDataSourceDTO commonDataSourceDTO = conversionService.convert(dataSource, CommonDataSourceDTO.class);
            commonDataSourceDTO.setModelType(autoDetectedFields.getCommonModelType());
            commonDataSourceDTO.setCdmVersion(autoDetectedFields.getCdmVersion());

            commonDataSourceDTO.setDbmsType(dataSource.getType());

            CommonDataSourceDTO centralDTO = integrationService.sendDataSourceCreationRequest(
                    owner,
                    dataSource.getDataNode(),
                    commonDataSourceDTO
            );
            dataSource.setCentralId(centralDTO.getId());

            checkNotNull(centralDTO.getId(), "central id of datasource is null");
            dataSourceRepository.save(dataSource);
        }
    }

    @Override
    public List<DataSource> findAllNotDeleted() {

        return dataSourceRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public List<DataSource> findAllNotDeleted(String sortBy, Boolean sortAsc) {

        return dataSourceRepository.findAllByDeletedAtIsNull(getSort(sortBy, sortAsc));
    }

    @Override
    public void delete(Long id) {

        checkNotNull(id, "given data source surrogate id is blank ");
        dataSourceRepository.delete(id);
    }

    @Override
    public void delete(DataSource dataSource) {

        checkNotNull(dataSource, "given datasource is null");
        dataSourceRepository.delete(dataSource);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataSource> findByCentralId(Long centralId) {

        Preconditions.checkArgument(Objects.nonNull(centralId), "given data source centralId is null");
        return dataSourceRepository.findByCentralId(centralId);
    }

    @Override
    public DataSource getById(Long id) {

        checkNotNull(id, "given data source surrogate id is blank ");
        return dataSourceRepository.findById(id).orElseThrow(() ->
                new NotExistException("Data source " + id + " was not found", DataSource.class));
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataSource update(User user, DataSource dataSource) {

        final DataSource exists = getById(dataSource.getId());

        final String name = dataSource.getName();
        // Prevents name update in Standalone mode when datasource is synchronized
        if (Objects.equals(STANDALONE, dataNodeService.getDataNodeMode()) && exists.getCentralId() != null && !Objects.equals(name, exists.getName())) {
            throw new ValidationException("Cannot change Data source name which is published to Central in Standalone mode");
        }
        if (Objects.nonNull(name)) {
            exists.setName(name);
        }
        final DBMSType type = dataSource.getType();
        if (Objects.nonNull(type)) {
            exists.setType(type);
        }
        final String cdmSchema = dataSource.getCdmSchema();
        if (Objects.nonNull(cdmSchema)) {
            exists.setCdmSchema(cdmSchema);
        }
        final String connectionString = dataSource.getConnectionString();
        if (Objects.nonNull(connectionString)) {
            exists.setConnectionString(connectionString);
        }
        final String description = dataSource.getDescription();
        if (Objects.nonNull(description)) {
            exists.setDescription(description);
        }
        final String password = dataSource.getPassword();
        if (isNotDummyPassword(password)) {
            exists.setPassword(password);
        }
        final String username = dataSource.getUsername();
        if (Objects.nonNull(username)) {
            exists.setUsername(username);
        }
        final String atlasResultDbSchema = dataSource.getResultSchema();
        if (Objects.nonNull(atlasResultDbSchema)) {
            exists.setResultSchema(atlasResultDbSchema);
        }
        final String atlasTargetDbSchema = dataSource.getTargetSchema();
        if (Objects.nonNull(atlasTargetDbSchema)) {
            exists.setTargetSchema(atlasTargetDbSchema);
        }
        final String atlasTargetCohortTable = dataSource.getCohortTargetTable();
        if (Objects.nonNull(atlasTargetCohortTable)) {
            exists.setCohortTargetTable(atlasTargetCohortTable);
        }
        if (DBMSType.IMPALA.equals(type)) {
            final Boolean useKerberos = dataSource.getUseKerberos();
            if (Objects.nonNull(useKerberos)) {
                exists.setUseKerberos(useKerberos);
            }
            final String krbRealm = dataSource.getKrbRealm();
            if (Objects.nonNull(krbRealm)) {
                exists.setKrbRealm(krbRealm);
            }
            final String krbFQDN = dataSource.getKrbFQDN();
            if (Objects.nonNull(krbFQDN)) {
                exists.setKrbFQDN(krbFQDN);
            }
            final String krbUser = dataSource.getKrbUser();
            if (Objects.nonNull(krbUser)) {
                exists.setKrbUser(krbUser);
            }
            final String krbPassword = dataSource.getKrbPassword();
            if (isNotDummyPassword(krbPassword)) {
                exists.setKrbPassword(krbPassword);
            }
        } else {
            exists.setKrbRealm(null);
            exists.setKrbFQDN(null);
            exists.setKrbUser(null);
            exists.setKrbPassword(null);
        }

        final byte[] keytab = dataSource.getKeyfile();
        if (DBMSType.BIGQUERY.equals(type) || DBMSType.IMPALA.equals(type)) {
            if (Objects.nonNull(keytab)) {
                exists.setKeyfile(keytab);
            }
        } else {
            exists.setKeyfile(null);
        }

        DataSource updated = dataSourceRepository.save(exists);

        eventPublisher.publishEvent(new DataSourceUpdatedEvent(this, user, updated));

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOnCentral(User user,
                                DataSource dataSource) {

        AutoDetectedFields autoDetectedFields = autoDetectFields(dataSource);
        if (Objects.nonNull(dataSource.getCentralId())) {
            dataSourceRepository.save(dataSource);

            CommonDataSourceDTO commonDataSourceDTO = conversionService.convert(dataSource, CommonDataSourceDTO.class);
            commonDataSourceDTO.setModelType(autoDetectedFields.getCommonModelType());
            commonDataSourceDTO.setCdmVersion(autoDetectedFields.getCdmVersion());
            if (Objects.equals(NETWORK, dataNodeService.getDataNodeMode())) {
                integrationService.sendDataSourceUpdateRequest(
                        user,
                        dataSource.getCentralId(),
                        commonDataSourceDTO
                );
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateHealthStatus(Long centralId, CommonHealthStatus status, String description) {

        findByCentralId(centralId).ifPresent(dataSource -> {
            dataSource.setHealthStatus(status);
            dataSource.setHealthStatusDescription(description);
            dataSourceRepository.save(dataSource);
        });
    }

    @Override
    public AutoDetectedFields autoDetectFields(DataSource dataSource) {

        return new AutoDetectedFields(CommonModelType.CDM);
    }

    @Override
    public void removeKeytab(DataSource dataSource) {

        dataSource.setKeyfile(null);
        dataSourceRepository.save(dataSource);
    }

    protected final Sort getSort(String sortBy, Boolean sortAsc) {

        String defaultSort = "name";
        return new Sort(
                sortAsc == null || sortAsc ? Sort.Direction.ASC : Sort.Direction.DESC,
                dsSortPath.getOrDefault(sortBy, defaultSort)
        );
    }

    @Override
    public JsonResult unpublishAndDeleteOnCentral(Long dataSourceId) {

        DataSource dataSource = getById(dataSourceId);
        if (Objects.nonNull(dataSource.getCentralId())) {
            DataNodeUtils.requireNetworkMode(dataNodeService);
            return centralClient.unpublishAndSoftDeleteDataSource(dataSource.getCentralId());
        }
        return new JsonResult(NO_ERROR);
    }

    @Override
    public List<DataSource> findStandaloneSources() {

        return dataSourceRepository.findAllByCentralIdIsNull().collect(Collectors.toList());
    }
}
