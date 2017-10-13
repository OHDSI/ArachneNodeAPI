/**
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
 * Created: December 16, 2016
 *
 */

package com.odysseusinc.arachne.portal.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthenticationRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Created by AKrutov on 20.10.2016.
 */
public class BaseTest {

    protected String base;
    protected RestTemplate template;
    protected String authToken;

    @Value("${local.server.port}")
    protected int port;


    @Value("${datanode.arachneCentral.authHeader}")
    protected String tokenHeader;

    private ObjectMapper mapper = new ObjectMapper();


    protected void login() throws IOException {

        String username = "admin@admin.ru";
        String password = "password";
        login(username, password);
    }

    protected void login(String username, String password) {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");

        template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        CommonAuthenticationRequest authenticationRequest = new CommonAuthenticationRequest();
        authenticationRequest.setUsername(username);
        authenticationRequest.setPassword(password);
        HttpEntity request = new HttpEntity(authenticationRequest, headers);
        ResponseEntity<JsonResult> responseEntity = template.exchange(base + "auth/login", HttpMethod.POST,
                request, JsonResult.class);
        Map<String, Object> authenticationResponse = (Map<String, Object>) responseEntity.getBody().getResult();
        authToken = (String) authenticationResponse.get("token");
    }

}
