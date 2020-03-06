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
 * Created: August 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.portal;

import static com.odysseusinc.arachne.datanode.Constants.CentralApi.Achilles.LIST_PERMISSIONS;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.Achilles.LIST_REPORTS;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.Achilles.PERMISSION;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.DataNode.BUILD_NUMBER;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.User.LINK_TO_NODE;

import com.odysseusinc.arachne.commons.api.v1.dto.AtlasShortDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAchillesGrantTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAchillesPermissionDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAchillesReportDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonBuildNumberDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataNodeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityRequestDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonLinkUserToDataNodeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonListEntityRequest;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonListEntityResponseDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.Constants;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

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

    @RequestLine("POST /api/v1/data-nodes/common-entity/{id}")
    @Headers("Content-Type: multipart/form-data")
    void sendCommonEntityResponseEx(@Param("id") String id, @Param("files") MultipartFile[] files, @Param("properties") Map<String,String> properties);

    @RequestLine("POST /api/v1/data-nodes/atlases")
    @Headers("Content-Type: application/json")
    AtlasShortDTO updateAtlasInfo(AtlasShortDTO atlasInfo);

    @RequestLine("DELETE /api/v1/data-nodes/atlases/{id}")
    void deleteAtlas(@Param("id") Long atlasId);

    @RequestLine("POST /api/v1/achilles/datanode/datasource/{id}")
    @Headers("Content-Type: multipart/form-data")
    void sendAchillesResults(@Param("id") Long centralId, @Param("file") MultipartFile file);

    @RequestLine("GET /api/v1/data-sources/byuuid/{uuid}")
    JsonResult<CommonDataSourceDTO> getDataSource(@Param("uuid") String dataSourceUuid);

    @RequestLine("GET /api/v1/data-nodes/byuuid/{uuid}")
    JsonResult<CommonDataNodeDTO> getDataNode(@Param("uuid") String dataNodeUuid);

    @RequestLine("GET " + Constants.CentralApi.DataSource.GET)
    <T extends CommonDataSourceDTO> JsonResult<T> getDataSource(@Param("id") Long dataSourceId);

    @RequestLine("GET " + LIST_REPORTS)
    List<CommonAchillesReportDTO> listReports();

    @RequestLine("GET " + LIST_PERMISSIONS)
    List<CommonAchillesPermissionDTO> listPermissions(@Param("id") Long centralId);

    @RequestLine("GET " + PERMISSION)
    CommonAchillesPermissionDTO getPermission(@Param("dataSourceId") Long centralId, @Param("id") String reportId);

    @RequestLine("POST " + PERMISSION)
    @Headers("Content-Type: application/json")
    void setPermission(@Param("dataSourceId") Long centralId, @Param("id") String reportId, CommonAchillesGrantTypeDTO grantType);

    @RequestLine("POST " + LINK_TO_NODE)
    @Headers("Content-Type: application/json")
    void linkUser(@Param("datanodeId") Long centralId, CommonLinkUserToDataNodeDTO userLink);

    @RequestLine("DELETE " + LINK_TO_NODE)
    @Headers("Content-Type: application/json")
    void unlinkUser(@Param("datanodeId") Long centralId, CommonLinkUserToDataNodeDTO userLink);

    @RequestLine("PUT " + LINK_TO_NODE)
    @Headers("Content-Type: application/json")
    JsonResult<List<CommonUserDTO>> relinkUsers(@Param("datanodeId") Long centralId, List<CommonLinkUserToDataNodeDTO> userLinks);

    @RequestLine("GET " + BUILD_NUMBER)
    CommonBuildNumberDTO getBuildNumber();
}
