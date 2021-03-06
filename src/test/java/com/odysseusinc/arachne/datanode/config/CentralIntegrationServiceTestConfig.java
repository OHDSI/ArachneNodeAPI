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

import com.odysseusinc.arachne.datanode.service.CentralIntegrationService;
import org.mockito.Mockito;
import org.ohdsi.authenticator.service.authentication.AccessTokenResolver;
import org.ohdsi.authenticator.service.authentication.AuthenticationMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class CentralIntegrationServiceTestConfig {

    @Primary
    @Bean
    public CentralIntegrationService centralIntegrationService() {

        return Mockito.mock(CentralIntegrationService.class);
    }

    @Bean
    public AccessTokenResolver accessTokenResolver() {

        return new AccessTokenResolver("jwtTokenHeader", AuthenticationMode.STANDARD);
    }
}
