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
 * Created: August 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.portal;

import com.odysseusinc.arachne.commons.api.v1.dto.AtlasInfoDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataNodeRegisterResponseDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityRequestDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonListEntityRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonListEntityResponseDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CentralSystemClient {


    @RequestLine("GET /api/v1/data-nodes/entity-lists/requests")
    CommonListEntityRequest getEntityListRequests();

    @RequestLine("POST /api/v1/data-nodes/entity-lists/responses")
    @Headers("Content-Type: application/json")
    void sendListEntityResponse(CommonListEntityResponseDTO entityRequestResult);

    @RequestLine("GET /api/v1/data-nodes/entities")
    List<CommonEntityRequestDTO> getEntityRequests();

    @RequestLine("POST /api/v1/data-nodes/common-entity/{id}")
    @Headers("Content-Type: multipart/form-data")
    void sendCommonEntityResponse(@Param("id") String id, @Param("files") MultipartFile[] files);

    @RequestLine("POST /api/v1/data-nodes/atlas")
    @Headers("Content-Type: application/json")
    void sendAtlasInformation(AtlasInfoDTO atlasInfo);

    @RequestLine("POST /api/v1/achilles/datanode/datasource/{id}")
    @Headers("Content-Type: multipart/form-data")
    void sendAchillesResults(@Param("id") Long centralId, @Param("file") MultipartFile file);

    @RequestLine("GET /api/v1/data-sources/byuuid/{uuid}")
    JsonResult<CommonDataSourceDTO> getDataSource(@Param("uuid") String dataSourceUuid);

    @RequestLine("GET /api/v1/data-nodes/byuuid/{uuid}")
    JsonResult<CommonDataNodeRegisterResponseDTO> getDataNode(@Param("uuid") String dataNodeUuid);

    @RequestLine("GET /api/v1/data-nodes/{id}")
    JsonResult<CommonDataNodeRegisterResponseDTO> getDataNode(@Param("id") Long centralId);
}
