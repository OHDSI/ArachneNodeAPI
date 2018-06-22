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
 * Created: November 18, 2016
 *
 */

package com.odysseusinc.arachne.datanode.util;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class CentralUtil {

    @Value("${datanode.arachneCentral.host}")
    private String host;

    @Value("${datanode.arachneCentral.port}")
    private int port;

    @Value("${datanode.arachneCentral.authHeader}")
    private String authHeader;

    @Value("${datanode.arachneCentral.nodeAuthHeader}")
    private String nodeAuthHeader;

    public String getCentralUrl() {

        return host + ":" + port;
    }

    public HttpHeaders getCentralAuthHeader(String token) {

        return getHttpHeaders(authHeader, token);
    }

    public HttpHeaders getCentralNodeAuthHeader(String token) {

        return getHttpHeaders(nodeAuthHeader, token);
    }

    private HttpHeaders getHttpHeaders(String authHeader, String token) {

        checkArgument(isNotBlank(token), "given token is blank");
        HttpHeaders headers = new HttpHeaders();
        headers.add(authHeader, token);
        return headers;
    }
}
