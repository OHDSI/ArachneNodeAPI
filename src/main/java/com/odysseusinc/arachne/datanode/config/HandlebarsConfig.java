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
 * Created: August 02, 2017
 *
 */

package com.odysseusinc.arachne.datanode.config;

import static com.odysseusinc.arachne.commons.utils.TemplateUtils.loadTemplate;

import com.github.jknack.handlebars.Template;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class HandlebarsConfig {

    public static final String INCIDENCE_RATES_RUNNER_TEMPLATE = "ir/main.r";
    private static final String ESTIMATION_RUNNER_TEMPLATE = "estimation/runner.mustache";
    private static final String PATIENT_LEVEL_PREDICTION_RUNNER_TEMPLATE = "plp/main.r";
    private static final String COHORT_HERACLES_RUNNER_TEMPLATE = "heracles/main.r";
    private static final String COHORT_CHARACTERIZATION_TEMPLATE = "cc/runAnalysis.R";
    private static final String PATHWAYS_RUNNER_TEMPLATE = "pathways/main.R";

    @Bean
    public Template estimationRunnerTemplate() {

        return loadTemplate(ESTIMATION_RUNNER_TEMPLATE);
    }

    @Bean
    public Template patientLevelPredictionRunnerTemplate() {

        return loadTemplate(PATIENT_LEVEL_PREDICTION_RUNNER_TEMPLATE);
    }

    @Bean
    public Template cohortHeraclesRunnerTemplate() {

        return loadTemplate(COHORT_HERACLES_RUNNER_TEMPLATE);
    }

    @Bean
    public Template incidenceRatesRunnerTemplate(){

        return loadTemplate(INCIDENCE_RATES_RUNNER_TEMPLATE);
    }

    @Bean
    public Template cohortCharacterizationTemplate() {

        return loadTemplate(COHORT_CHARACTERIZATION_TEMPLATE);
    }

    @Bean
    public Template pathwaysRunnerTemplate() {

        return loadTemplate(PATHWAYS_RUNNER_TEMPLATE);
    }

}
