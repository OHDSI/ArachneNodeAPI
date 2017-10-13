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
 * Created: May 18, 2017
 *
 */

package com.odysseusinc.arachne.datanode.config.properties;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@ConfigurationProperties(prefix = "achilles")
@Validated
public class AchillesProperties {

    @NotNull
    private String imageName;

    private AuthConfig authConfig = new AuthConfig();

    private RetryConfig retry = new RetryConfig();

    public String getImageName() {

        return imageName;
    }

    public AuthConfig getAuthConfig() {

        return authConfig;
    }

    public void setImageName(String imageName) {

        this.imageName = imageName;
    }

    public RetryConfig getRetry() {

        return retry;
    }

    public static class AuthConfig {
        private String registryAddress;
        private String username;
        private String password;

        public String getRegistryAddress() {

            return registryAddress;
        }

        public void setRegistryAddress(String registryAddress) {

            this.registryAddress = registryAddress;
        }

        public String getUsername() {

            return username;
        }

        public void setUsername(String username) {

            this.username = username;
        }

        public String getPassword() {

            return password;
        }

        public void setPassword(String password) {

            this.password = password;
        }
    }

    public static class RetryConfig {
        private int maxAttempts = 5;

        private BackOffPolicyConfig backoff = new BackOffPolicyConfig();

        public int getMaxAttempts() {

            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {

            this.maxAttempts = maxAttempts;
        }

        public BackOffPolicyConfig getBackoff() {

            return backoff;
        }
    }

    public static class BackOffPolicyConfig {

        private long initialInterval = 100L;
        private long maxInterval = 30000L;
        private double multiplier = 2.0D;

        public long getInitialInterval() {

            return initialInterval;
        }

        public void setInitialInterval(long initialInterval) {

            this.initialInterval = initialInterval;
        }

        public long getMaxInterval() {

            return maxInterval;
        }

        public void setMaxInterval(long maxInterval) {

            this.maxInterval = maxInterval;
        }

        public double getMultiplier() {

            return multiplier;
        }

        public void setMultiplier(double multiplier) {

            this.multiplier = multiplier;
        }
    }
}
