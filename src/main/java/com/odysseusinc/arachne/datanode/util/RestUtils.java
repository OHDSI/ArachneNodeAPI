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
 * Created: Jul 8, 2019
 *
 */

package com.odysseusinc.arachne.datanode.util;

import com.odysseusinc.arachne.datanode.exception.BadRequestException;
import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;

public class RestUtils {

    private RestUtils() {

    }

    private static final String DEFAULT_AUTH_SCHEME = "Bearer";

    public static HttpEntity<LinkedMultiValueMap<String, Object>> createExecutionEngineRequestEntity(
            AnalysisRequestDTO analysisRequestDTO,
            File archive,
            String executionEngineToken, boolean compressedResult, boolean healthCheck) {

        HttpHeaders jsonHeader = new HttpHeaders();
        jsonHeader.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AnalysisRequestDTO> analysisRequestHttpEntity
                = new HttpEntity<>(analysisRequestDTO, jsonHeader);
        LinkedMultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
        multipartRequest.add("analysisRequest", analysisRequestHttpEntity);

        multipartRequest.add("file", new FileSystemResource(archive));

        HttpHeaders headers = new HttpHeaders();
        headers.add("arachne-compressed", String.valueOf(true));
        if (compressedResult) {
            headers.add("arachne-waiting-compressed-result", String.valueOf(true));
        }
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (healthCheck) {
            headers.add("arachne-attach-cdm-metadata", String.valueOf(false));
        }
        if (!StringUtils.isEmpty(executionEngineToken)) {
            headers.set("Authorization", checkCredentials(executionEngineToken));
        }
        return new HttpEntity<>(multipartRequest, headers);
    }

    public static String checkCredentials(final String token) {

        if (StringUtils.isNotBlank(token)) {
            String[] parts = token.split("\\s+");
            String authScheme;
            String authParams;
            if (parts.length > 1) {
                authScheme = parts[0];
                authParams = parts[1];
            } else {
                authScheme = DEFAULT_AUTH_SCHEME;
                authParams = token;
            }
            return authScheme + " " + authParams;
        } else {
            return "";
        }
    }

    public static void requireNetworkMode(FunctionalMode currentMode) {

        if (!FunctionalMode.NETWORK.equals(currentMode)) {
            throw new BadRequestException();
        }
    }
}
