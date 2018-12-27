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
 * Created: June 13, 2017
 *
 */

package com.odysseusinc.arachne.datanode;

import com.odysseusinc.arachne.datanode.config.WebSecurityConfig;
import javax.annotation.Resource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = {
        "com.odysseusinc.arachne.datanode.repository",
        "com.odysseusinc.arachne.system.settings.repository"
})
@EntityScan(basePackages = {"com.odysseusinc.arachne.*"})
@ComponentScan(basePackages = {"com.odysseusinc.arachne.datanode"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebApplicationStarter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebSecurityConfig.class)
        })
@Configuration
@PropertySource("classpath:application.properties")
@Import(MailSenderAutoConfiguration.class)
public class TestApplication {

    @Resource
    private Environment environment;

    public static void main(String[] args) {

        SpringApplication.run(TestApplication.class, args);
    }

   /* @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDataSource() {

        return DataSourceBuilder.create().build();
    }*/
}
