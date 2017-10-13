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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docker")
public class DockerProperties {

    private String host = "unix:///var/run/docker.sock";
    private boolean tlsVerify = false;
    private String certPath;
    private Registry registry = new Registry();
    private CmdExecFactory cmdExecFactory = new CmdExecFactory();

    public CmdExecFactory getCmdExecFactory() {

        return cmdExecFactory;
    }

    public void setCmdExecFactory(CmdExecFactory cmdExecFactory) {

        this.cmdExecFactory = cmdExecFactory;
    }

    public Registry getRegistry() {

        return registry;
    }

    public void setRegistry(Registry registry) {

        this.registry = registry;
    }

    public String getHost() {

        return host;
    }

    public void setHost(String host) {

        this.host = host;
    }

    public boolean isTlsVerify() {

        return tlsVerify;
    }

    public void setTlsVerify(boolean tlsVerify) {

        this.tlsVerify = tlsVerify;
    }

    public String getCertPath() {

        return certPath;
    }

    public void setCertPath(String certPath) {

        this.certPath = certPath;
    }

    public static class Registry {

        private String host;
        private String username;
        private String password;

        public String getHost() {

            return host;
        }

        public void setHost(String host) {

            this.host = host;
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

    public static class CmdExecFactory {
        private int readTimeout = 1000;
        private int connectTimeout = 1000;
        private int maxTotalConnections = 100;
        private int maxPerRouteConnections = 10;

        public int getReadTimeout() {

            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {

            this.readTimeout = readTimeout;
        }

        public int getConnectTimeout() {

            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {

            this.connectTimeout = connectTimeout;
        }

        public int getMaxTotalConnections() {

            return maxTotalConnections;
        }

        public void setMaxTotalConnections(int maxTotalConnections) {

            this.maxTotalConnections = maxTotalConnections;
        }

        public int getMaxPerRouteConnections() {

            return maxPerRouteConnections;
        }

        public void setMaxPerRouteConnections(int maxPerRouteConnections) {

            this.maxPerRouteConnections = maxPerRouteConnections;
        }
    }
}
