package com.odysseusinc.arachne.datanode.config;

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
 * Created: May 18, 2017
 *
 */

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.odysseusinc.arachne.datanode.config.properties.DockerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DockerProperties.class)
public class DockerConfig {
    @Bean
    public DockerClientConfig dockerClient(DockerProperties properties) {

        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(properties.getHost())
                .withDockerTlsVerify(properties.isTlsVerify())
                .withDockerCertPath(properties.getCertPath())
                .withRegistryUrl(properties.getRegistry().getHost())
                .withRegistryUsername(properties.getRegistry().getUsername())
                .withRegistryPassword(properties.getRegistry().getPassword())
                .build();
    }
}