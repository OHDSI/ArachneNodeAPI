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
 * Created: December 16, 2016
 *
 */

//package com.odysseusinc.arachne.portal.test;
//
//import com.odysseusinc.arachne.commons.portal.AuthenticationRequest;
//import com.odysseusinc.arachne.commons.portal.AuthenticationResponse;
//import com.odysseusinc.arachne.commons.util.JsonResult;
//import com.odysseusinc.arachne.datanode.WebApplicationStarter;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.FixMethodOrder;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.MethodSorters;
//import org.springframework.boot.test.SpringApplicationConfiguration;
//import org.springframework.boot.test.WebIntegrationTest;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestTemplate;
//
///**
// * Created by AKrutov on 23.06.2016.
// */
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(WebApplicationStarter.class)
//@WebIntegrationTest("server.port:9002")
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@TestPropertySource(locations = "classpath:config/application.yml")
//public class AuthTests extends BaseTest {
//
//    @Before
//    public void setUp() throws Exception {
//        this.base = "http://localhost:" + port + "/api/v1/";
//        template = new RestTemplate();
//
////
////        User admin = userService.getByEmail("admin@admin.ru");
////        if (admin != null && (System.currentTimeMillis() - admin.getCreated().getTime() > 10000)) {
////            userService.remove(admin.getId());
////            admin = null;
////        }
////        if (admin == null) {
////            User user = new User();
////            user.setEmail("admin@admin.ru");
////            user.setCreated(new Date());
////            user.setUpdated(new Date());
////            user.setEnabled(true);
////            user.setPassword(passwordEncoder.encode("password"));
////            user.setLastPasswordReset(new Date());
////            user.setProfessionalType(getProffesionalType());
////            userService.create(user);
////        }
//
//    }
//
////    @Test
////    public void test1Login() throws Exception {
////
////        login();
////        Assert.assertNotNull(authToken);
////
////    }
////
////    @Test
////    public void test2LoginFail() throws Exception {
////        try {
////            RestTemplate restTemplate = new RestTemplate();
////            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
////            headers.add("Content-Type", "application/json");
////
////            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
////            AuthenticationRequest authenticationRequest = new AuthenticationRequest();
////            authenticationRequest.setUsername("admin@admin.ru");
////            authenticationRequest.setPassword("password1");
////            HttpEntity request = new HttpEntity(authenticationRequest, headers);
////            AuthenticationResponse result = template.postForObject(base + "auth/login", request, AuthenticationResponse.class);
////        } catch (HttpClientErrorException e) {
////            Assert.assertEquals(e.getRawStatusCode(), 401);
////        }
////    }
//
////    @Test
////    public void test3expired() throws Exception {
////
////        login();
////        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
////        headers.add(tokenHeader, authToken);
////        headers.add("Content-Type", "application/json");
////        HttpEntity request = new HttpEntity(headers);
////        try {
////            headers = new LinkedMultiValueMap<>();
////            template = new RestTemplate();
////            headers.add(tokenHeader, authToken);
////            headers.add("Content-Type", "application/json");
////            request = new HttpEntity(headers);
////            template.exchange(base + "test", HttpMethod.GET, request, JsonResult.class);
////        } catch (HttpClientErrorException e) {
////            Assert.assertEquals(e.getRawStatusCode(), 404);
////        }
////    }
//
////    @Test
////    public void test3RefreshToken() throws Exception {
////        login();
////        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
////        headers.add(tokenHeader, authToken);
////        headers.add("Content-Type", "application/json");
////        HttpEntity request = new HttpEntity(headers);
////        ResponseEntity<JsonResult> responseEntity = template.exchange(base + "auth/refresh", HttpMethod.GET, request, JsonResult.class);
////        authToken = (String) responseEntity.getBody().getResult();
////
////        try {
////            headers = new LinkedMultiValueMap<>();
////            template = new RestTemplate();
////            headers.add(tokenHeader, authToken);
////            headers.add("Content-Type", "application/json");
////            request = new HttpEntity(headers);
////            template.exchange(base + "test", HttpMethod.GET, request, JsonResult.class);
////        } catch (HttpClientErrorException e) {
////            Assert.assertEquals(e.getRawStatusCode(), 404);
////        }
////
////    }
////
////    @Test
////    public void test4Logout() throws Exception {
////        login();
////        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
////        headers.add(tokenHeader, authToken);
////        headers.add("Content-Type", "application/json");
////        HttpEntity request = new HttpEntity(headers);
////        template.exchange(base + "auth/logout", HttpMethod.POST, request, JsonResult.class);
////
////        try {
////            headers = new LinkedMultiValueMap<>();
////            template = new RestTemplate();
////            headers.add(tokenHeader, authToken);
////            headers.add("Content-Type", "application/json");
////            request = new HttpEntity(headers);
////            template.exchange(base + "test", HttpMethod.GET, request, JsonResult.class);
////        } catch (HttpClientErrorException e) {
////            Assert.assertEquals(e.getRawStatusCode(), 401);
////        }
////
////    }
//}
//
//
