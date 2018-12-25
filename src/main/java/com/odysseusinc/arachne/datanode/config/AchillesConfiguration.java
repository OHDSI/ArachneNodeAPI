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

package com.odysseusinc.arachne.datanode.config;

import com.odysseusinc.arachne.datanode.config.properties.AchillesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(AchillesProperties.class)
public class AchillesConfiguration {

    @Bean
    public RetryTemplate achillesRetryTemplate(AchillesProperties properties) {

        RetryTemplate retryTemplate = new RetryTemplate();
        AchillesProperties.RetryConfig retryConfig = properties.getRetry();
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RestClientException.class, true);
        SimpleRetryPolicy policy = new SimpleRetryPolicy(retryConfig.getMaxAttempts(),
                    retryableExceptions);
        retryTemplate.setRetryPolicy(policy);
        AchillesProperties.BackOffPolicyConfig backOffPolicyConfig = retryConfig.getBackoff();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(backOffPolicyConfig.getInitialInterval());
        backOffPolicy.setMaxInterval(backOffPolicyConfig.getMaxInterval());
        backOffPolicy.setMultiplier(backOffPolicyConfig.getMultiplier());
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
