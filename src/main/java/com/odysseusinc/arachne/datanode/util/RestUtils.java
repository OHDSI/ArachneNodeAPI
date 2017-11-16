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
 * Created: July 11, 2017
 *
 */

package com.odysseusinc.arachne.datanode.util;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.util.CommonFileUtils;
import java.io.File;
import java.util.Collection;
import java.util.List;
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
            File analysisFolder,
            String executionEngineToken, boolean compressedResult, boolean healthCheck) {

        final List<File> files = com.odysseusinc.arachne.commons.utils.CommonFileUtils.getFiles(analysisFolder);
        Collection<FileSystemResource> fsResources = CommonFileUtils.getFSResources(files);
        HttpHeaders jsonHeader = new HttpHeaders();
        jsonHeader.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AnalysisRequestDTO> analysisRequestHttpEntity
                = new HttpEntity<>(analysisRequestDTO, jsonHeader);
        LinkedMultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
        multipartRequest.add("analysisRequest", analysisRequestHttpEntity);
        if (fsResources != null) {
            fsResources.forEach(f -> multipartRequest.add("file", f));
        }
        HttpHeaders headers = new HttpHeaders();
        if (compressedResult) {
            headers.add("arachne-waiting-compressed-result", String.valueOf(true));
        }
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (healthCheck) {
            headers.add("arachne-datasource-check", String.valueOf(true));
        }
        if (!StringUtils.isEmpty(executionEngineToken)) {
            headers.set("Authorization", checkCredentials(executionEngineToken));
        }
        return new HttpEntity<>(multipartRequest, headers);
    }

    private static String checkCredentials(final String token) {

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
}
