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
 * Created: December 19, 2016
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonHealthStatus;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.repository.DataSourceRepository;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.data.domain.Sort;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DBMSType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DataSourceServiceImpl implements DataSourceService {

    private static final String DATANODE_IS_NOT_EXIST_EXCEPTION = "DataNode entry is not exist, create it before";

    private final DataSourceRepository dataSourceRepository;
    private final DataNodeService dataNodeService;
    private final Map<String, String> dsSortPath = new HashMap<>();

    @Autowired
    public DataSourceServiceImpl(DataSourceRepository dataSourceRepository,
                                 DataNodeService dataNodeService) {

        this.dataSourceRepository = dataSourceRepository;
        this.dataNodeService = dataNodeService;
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
    public Optional<DataSource> create(User owner, DataSource dataSource) throws NotExistException {

        Optional<DataNode> currentDataNode = dataNodeService.findCurrentDataNode();
        if (!currentDataNode.isPresent()) {
            throw new NotExistException(DATANODE_IS_NOT_EXIST_EXCEPTION, DataNode.class);
        }
        checkNotNull(dataSource, "given datasource is null");
        checkNotNull(owner, "given owner is null");
        dataSource.setUuid(UUID.randomUUID().toString());
        dataSource.setDataNode(currentDataNode.get());
        dataSource.setRegistred(false);
        return Optional.of(dataSourceRepository.save(dataSource));
    }

    @Override
    public List<DataSource> findAll() {

        return dataSourceRepository.findAll();
    }

    @Override
    public List<DataSource> findAll(String sortBy, Boolean sortAsc) {

        return dataSourceRepository.findAll(getSort(sortBy, sortAsc));
    }

    @Override
    public List<DataSource> findAllRegistered() {

        return dataSourceRepository.findAllRegistered();
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
    public Optional<DataSource> findBySid(String sid) {

        Preconditions.checkArgument(StringUtils.isNotBlank(sid), "given data source surrogate sid is blank ");
        return dataSourceRepository.findByUuid(sid);
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

        final DataSource exists = dataSourceRepository.findById(dataSource.getId())
                .orElseThrow(() -> new NotExistException(DataSource.class));

        final String name = dataSource.getName();
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
        if (Objects.nonNull(password)) {
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
        return dataSourceRepository.save(exists);
    }

    @Transactional
    @Override
    public DataSource markDataSourceAsRegistered(Long centralId) {

        return setDSRegistered(centralId, true);
    }

    @Transactional
    @Override
    public DataSource markDataSourceAsUnregistered(Long centralId) {

        return setDSRegistered(centralId, false);
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

    private DataSource setDSRegistered(Long centralId, boolean registered) {

        DataSource forUpdate = dataSourceRepository.findByCentralId(centralId)
                .orElseThrow(() -> new NotExistException(DataSource.class));
        forUpdate.setRegistred(registered);
        dataSourceRepository.save(forUpdate);
        return forUpdate;
    }

    protected final Sort getSort(String sortBy, Boolean sortAsc) {

        String defaultSort = "name";
        return new Sort(
                sortAsc == null || sortAsc ? Sort.Direction.ASC : Sort.Direction.DESC,
                dsSortPath.getOrDefault(sortBy, defaultSort)
        );
    }
}
