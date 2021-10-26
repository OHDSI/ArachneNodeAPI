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
 * Created: Oct 1, 2019
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.testinfected.hamcrest.validation.ViolationMatchers.fails;
import static org.testinfected.hamcrest.validation.ViolationMatchers.on;
import static org.testinfected.hamcrest.validation.ViolationMatchers.succeeds;
import static org.testinfected.hamcrest.validation.ViolationMatchers.violates;
import static org.testinfected.hamcrest.validation.ViolationMatchers.violation;

import com.odysseusinc.arachne.TestContainersInitializer;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.datanode.dto.datasource.CreateDataSourceDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.KerberosAuthMechanism;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.multipart.MultipartFile;
import org.testinfected.hamcrest.validation.ViolationMatchers;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
@ContextConfiguration(initializers = TestContainersInitializer.class)
public class ValidatorTest {

    @Autowired
    private Validator validator;

    private MultipartFile mockKeyFile = new MockMultipartFile("keyfile.json", new byte[128]);

    @Test
    public void shouldValidateCreateDatasourceDTO() {

        //Should pass with keyfile
        CreateDataSourceDTO bqDataSourceDTO = prepareDataSourceDTO(DBMSType.BIGQUERY);
        bqDataSourceDTO.setKeyfile(mockKeyFile);
        Set<ConstraintViolation<CreateDataSourceDTO>> violations = validator.validate(bqDataSourceDTO);
        assertThat(violations, succeeds());

        //Should fail without username
        CreateDataSourceDTO pgDataSourceDTO = prepareDataSourceDTO(DBMSType.POSTGRESQL);
        violations = validator.validate(pgDataSourceDTO);
        assertThat(violations, fails());
        assertThat(violations, hasSize(1));
        assertThat(violations, violates(violation(on("dbUsername"))));

        //Should pass
        pgDataSourceDTO.setDbUsername("postgresql");
        violations = validator.validate(pgDataSourceDTO);
        assertThat(violations, succeeds());

        //Should fail without username not using kerberos
        CreateDataSourceDTO impalaDataSourceDTO = prepareDataSourceDTO(DBMSType.IMPALA);
        violations = validator.validate(impalaDataSourceDTO);
        assertThat(violations, fails());
        assertThat(violations, hasSize(1));
        assertThat(violations, violates(violation(on("dbUsername"))));

        //Should not fail with username not using kerberos
        impalaDataSourceDTO.setDbUsername("user");
        violations = validator.validate(impalaDataSourceDTO);
        assertThat(violations, succeeds());

        //Should pass with keyfile
        impalaDataSourceDTO.setKeyfile(mockKeyFile);
        violations = validator.validate(impalaDataSourceDTO);
        assertThat(violations, succeeds());

        //Should fail without kerberos user during kerberos password auth
        impalaDataSourceDTO.setUseKerberos(true);
        impalaDataSourceDTO.setKrbUser(null);
        impalaDataSourceDTO.setKeyfile(null);
        impalaDataSourceDTO.setKrbAuthMechanism(KerberosAuthMechanism.PASSWORD);
        violations = validator.validate(impalaDataSourceDTO);
        assertThat(violations, fails());
        assertThat(violations, violates(violation(on("krbUser"))));
    }

    @Test
    public void shouldValidateDataSource() {

        //Should fail without keyfile
        DataSource bqDataSource = prepareDataSource(DBMSType.BIGQUERY);
        Set<ConstraintViolation<DataSource>> violations = validator.validate(bqDataSource);
        assertThat(violations, fails());
        assertThat(violations, hasSize(1));
        assertThat(violations, violates(violation(on("keyfile"))));

        //Should pass with keyfile
        bqDataSource.setKeyfile(new byte[16]);
        violations = validator.validate(bqDataSource);
        assertThat(violations, succeeds());

        //Should fail without username
        DataSource pgDataSource = prepareDataSource(DBMSType.POSTGRESQL);
        violations = validator.validate(pgDataSource);
        assertThat(violations, fails());
        assertThat(violations, hasSize(1));
        assertThat(violations, violates(violation(on("username"))));

        //Should pass
        pgDataSource.setUsername("postgresql");
        violations = validator.validate(pgDataSource);
        assertThat(violations, succeeds());

        //Should fail without username not using kerberos
        DataSource impalaDataSource = prepareDataSource(DBMSType.IMPALA);
        violations = validator.validate(impalaDataSource);
        assertThat(violations, fails());
        assertThat(violations, hasSize(1));
        assertThat(violations, violates(violation(on("username"))));

        //Should not fail with username not using kerberos
        impalaDataSource.setUsername("user");
        violations = validator.validate(impalaDataSource);
        assertThat(violations, succeeds());

        //Should fail without keyfile using kerberos keytab auth
        impalaDataSource.setUsername(null);
        impalaDataSource.setUseKerberos(true);
        impalaDataSource.setKrbAuthMechanism(KerberosAuthMechanism.KEYTAB);
        violations = validator.validate(impalaDataSource);
        assertThat(violations, fails());
        assertThat(violations, hasSize(1));
        assertThat(violations, violates(violation(on("keyfile"))));

        //Should pass with keyfile
        impalaDataSource.setKeyfile(new byte[16]);
        violations = validator.validate(impalaDataSource);
        assertThat(violations, succeeds());

        //Should fail without kerberos user during kerberos password auth
        impalaDataSource.setKeyfile(null);
        impalaDataSource.setKrbAuthMechanism(KerberosAuthMechanism.PASSWORD);
        violations = validator.validate(impalaDataSource);
        assertThat(violations, fails());
        assertThat(violations, violates(violation(on("krbUser"))));
    }

    private CreateDataSourceDTO prepareDataSourceDTO(DBMSType type) {

        CreateDataSourceDTO dataSourceDTO = new CreateDataSourceDTO();
        dataSourceDTO.setDbmsType(type.getValue());
        dataSourceDTO.setName("testDataSource");
        dataSourceDTO.setConnectionString("jdbc:postgresql://localhost/datanode_test");
        dataSourceDTO.setCdmSchema("default");
        return dataSourceDTO;
    }

    private DataSource prepareDataSource(DBMSType type) {

        DataSource dataSource = new DataSource();
        dataSource.setType(type);
        dataSource.setName("testDataSource");
        dataSource.setConnectionString("jdbc:postgresql://localhost/datanode_test");
        dataSource.setCdmSchema("default");
        dataSource.setUseKerberos(false);
        return dataSource;
    }

}
