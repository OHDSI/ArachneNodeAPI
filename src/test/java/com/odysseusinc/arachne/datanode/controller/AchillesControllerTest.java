/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
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

package com.odysseusinc.arachne.datanode.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.odysseusinc.arachne.commons.utils.ConverterUtils;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJob;
import com.odysseusinc.arachne.datanode.repository.AchillesJobRepository;
import com.odysseusinc.arachne.nohandlerfoundexception.NoHandlerFoundExceptionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        TransactionalTestExecutionListener.class})
@DbUnitConfiguration(databaseConnection = {"primaryDataSource"})
@DatabaseSetup(value = {
        "/data/achilles/datanode.xml",
        "/data/achilles/datasource.xml",
        "/data/achilles/achilles_jobs.xml"
}, type = DatabaseOperation.CLEAN_INSERT)
@Transactional
public class AchillesControllerTest {

    private static final Long DATASOURCE_ID = 1L;
    private static final int NUMBER_OF_JOBS = 2;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private AchillesJobRepository achillesJobRepository;

    private MockMvc mvc;

    @MockBean
    private ConverterUtils converterUtils;
    @MockBean
    private NoHandlerFoundExceptionUtils noHandlerFoundExceptionUtils;

    @Before
    public void setUp() throws Exception {

        mvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    //Should return SUCCESS job
    public void statusTest() throws Exception {

        mvc.perform(get(API.achillesJobStatus(DATASOURCE_ID))
            .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is(0)))
                .andExpect(jsonPath("$.result.status", is("SUCCESSFUL")));
    }

    @Test
    //Should return two jobs in history
    public void history() throws Exception {

        mvc.perform(get(API.achillesJobHistory(DATASOURCE_ID))
            .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is(0)))
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.content", hasSize(NUMBER_OF_JOBS)))
                .andExpect(jsonPath("$.result.content[0].dataSource.id", is(DATASOURCE_ID.intValue())))
                .andExpect(jsonPath("$.result.content[1].dataSource.id", is(DATASOURCE_ID.intValue())));
    }

    @Test
    //Should return test string as job log
    public void log() throws Exception {

        AchillesJob job = achillesJobRepository.findOne(2L);
        mvc.perform(get(API.achillesJobLog(DATASOURCE_ID, job.getStarted().getTime()))
        .accept(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errorCode", is(0)))
        .andExpect(jsonPath("$.result", is("test_string")));
    }

}