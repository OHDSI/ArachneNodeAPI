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
 * Created: February 09, 2017
 *
 */

package com.odysseusinc.arachne.datanode.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("public/**").addResourceLocations("classpath:/public/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        registry.addViewController("/dashboard**").setViewName("index");
        registry.addViewController("/study-notebook**").setViewName("index");
        registry.addViewController("/auth/login**").setViewName("index");
        registry.addViewController("/auth/register**").setViewName("index");
        registry.addViewController("/expert-finder**").setViewName("index");
        registry.addViewController("/data-catalog**").setViewName("index");
        registry.addViewController("/insights-library**").setViewName("index");
        registry.addViewController("/study-manager/studies/**").setViewName("index");
        registry.addViewController("analysis-execution/analyses/**").setViewName("index");
        registry.addViewController("/cdm-source-list/data-sources**").setViewName("index");
        registry.addViewController("/cdm-source-list/data-sources/**").setViewName("index");
        registry.addViewController("/admin-settings/**").setViewName("index");
        registry.addViewController("/external-resource-manager/**").setViewName("index");
    }

    @Bean
    public BeanPostProcessor commonAnnotationBeanPostProcessor() {

        return new CommonAnnotationBeanPostProcessor();
    }
}
