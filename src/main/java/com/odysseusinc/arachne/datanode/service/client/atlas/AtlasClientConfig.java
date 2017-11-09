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
 * Created: August 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.atlas;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtlasClientConfig {

    @Value("${atlas.host}")
    private String atlasHost;
    @Value("${atlas.port}")
    private Integer atlasPort;
    @Value("#{ @environment.getProperty('atlas.auth.schema') ?: T(com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthSchema).NONE}")
    private AtlasAuthSchema authSchema;
    @Value("#{ @environment.getProperty('atlas.auth.username') ?: null}")
    private String username;
    @Value("#{ @environment.getProperty('atlas.auth.password') ?: null}")
    private String password;

    @Bean
    public AtlasClient atlasClient() {

        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(AtlasClient.class))
                .logLevel(feign.Logger.Level.FULL)
                .requestInterceptor(new AtlasAuthRequestInterceptor(atlasLoginClient(), authSchema, username, password))
                .target(AtlasClient.class, atlasHost + ":" + atlasPort + "/WebAPI");
    }

    @Bean
    public AtlasLoginClient atlasLoginClient() {

        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new TokenDecoder())
                .logger(new Slf4jLogger(AtlasLoginClient.class))
                .target(AtlasLoginClient.class, atlasHost + ":" + atlasPort + "/WebAPI");
    }
}
