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
 * Created: October 18, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataNodeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.datanode.repository.DataNodeRepository;
import com.odysseusinc.arachne.datanode.repository.DataSourceRepository;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Transactional
@Profile("!test")
public class DataSourceUUIDMigration {

    private final PlatformTransactionManager transactionManager;
    private final DataNodeRepository dataNodeRepository;
    private final DataSourceRepository dataSourceRepository;
    private final CentralSystemClient centralClient;
    @Value("${migration.retry.timeout}")
    private long retryTimeout;

    @Autowired
    public DataSourceUUIDMigration(@Qualifier("transactionManager") PlatformTransactionManager transactionManager,
                                   DataNodeRepository dataNodeRepository,
                                   DataSourceRepository dataSourceRepository,
                                   CentralSystemClient centralClient) {

        this.transactionManager = transactionManager;
        this.dataNodeRepository = dataNodeRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.centralClient = centralClient;
    }

    @PostConstruct
    public void init() {

        getRetryTemplate().execute((context) -> {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

                    migrateDataNodes();
                    migrateDataSources();
                }
            });
            return null;
        });
    }

    public void migrateDataNodes() {

        dataNodeRepository.findAllByCentralIdIsNull().forEach(dn -> {
            CommonDataNodeDTO dataNodeRegisterResponseDTO = centralClient.getDataNode(dn.getSid()).getResult();
            if (dataNodeRegisterResponseDTO != null) {
                dn.setCentralId(dataNodeRegisterResponseDTO.getCentralId());
                dataNodeRepository.save(dn);
            }
        });
    }

    public void migrateDataSources() {

        dataSourceRepository.findAllByCentralIdIsNull().forEach(ds -> {
            CommonDataSourceDTO dto = centralClient.getDataSource(ds.getUuid()).getResult();
            if (dto != null) {
                ds.setCentralId(dto.getId());
                dataSourceRepository.save(ds);
            }
        });
    }

    private RetryTemplate getRetryTemplate() {

        RetryTemplate template = new RetryTemplate();
        RetryPolicy policy = new AlwaysRetryPolicy();
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000L);
        template.setBackOffPolicy(backOffPolicy);
        template.setRetryPolicy(policy);
        return template;
    }
}
