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

package com.odysseusinc.arachne.datanode.config;

import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import javax.validation.ValidatorFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class HibernateValidatorJpaConfig extends HibernateJpaAutoConfiguration {

    private ValidatorFactory validator;

    public HibernateValidatorJpaConfig(DataSource dataSource, JpaProperties jpaProperties,
                                       ObjectProvider<JtaTransactionManager> jtaTransactionManager,
                                       ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers,
                                       ValidatorFactory validator) {

        super(dataSource, jpaProperties, jtaTransactionManager, transactionManagerCustomizers);
        this.validator = validator;
    }

    @Override
    protected void customizeVendorProperties(Map<String, Object> vendorProperties) {

        super.customizeVendorProperties(vendorProperties);
        if (Objects.nonNull(validator)) {
            vendorProperties.put("javax.persistence.validation.factory", validator);
        }
    }
}
