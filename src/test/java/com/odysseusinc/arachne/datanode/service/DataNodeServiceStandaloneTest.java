/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Oct 16, 2019
 *
 */

package com.odysseusinc.arachne.datanode.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.user.User;
import javax.ws.rs.NotFoundException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "datanode.runMode=STANDALONE")
@ActiveProfiles("test")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        TransactionDbUnitTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DbUnitConfiguration(databaseConnection = "primaryDataSource")
@DatabaseTearDown(value = "/data/dataset/empty-datanode.xml", type = DatabaseOperation.DELETE_ALL)
public class DataNodeServiceStandaloneTest {
    @Autowired
    private DataNodeService dataNodeService;
    @Autowired
    private UserService userService;
    @Autowired
    private CentralIntegrationService centralIntegrationService;

    @Test
    @DatabaseSetup(value = "/data/dataset/empty-datanode.xml", type = DatabaseOperation.DELETE_ALL)
    @DatabaseSetup(value = "/data/dataset/users.xml", type = DatabaseOperation.CLEAN_INSERT)
    public void createSuccessStandaloneMode() {

        User user = userService.findByUsername(Const.USER).orElseThrow(NotFoundException::new);
        DataNode dataNode = new DataNode();
        Mockito.when(centralIntegrationService.sendDataNodeCreationRequest(user, dataNode))
                .then(invocationOnMock -> {
                    throw new RuntimeException("should not be called in Standalone mode");
                });
        DataNode created = dataNodeService.create(user, dataNode);
        assertThat(created, notNullValue());
        assertThat(created.getId(), greaterThan(0L));
        assertThat(created.getToken(), nullValue());
        assertThat(created.getCentralId(), nullValue());
    }

    @Test
    public void shouldNotBeInNetworkMode(){

        Assertions.assertThat(dataNodeService.isNetworkMode()).isFalse();
    }
}
