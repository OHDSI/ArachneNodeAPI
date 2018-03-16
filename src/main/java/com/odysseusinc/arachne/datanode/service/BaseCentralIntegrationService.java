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
 * Created: September 25, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service;

import com.odysseusinc.arachne.commons.api.v1.dto.ArachnePasswordInfoDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAuthMethodDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonBaseDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonDataSourceDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonProfessionalTypeDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonUserRegistrationDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.dto.user.CentralRegisterUserDTO;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

public interface BaseCentralIntegrationService<DS extends DataSource, DTO extends CommonBaseDataSourceDTO> {

    JsonResult<CommonAuthMethodDTO> getAuthMethod();

    DataNode sendDataNodeCreationRequest(User user, DataNode dataNode);

    DTO sendDataSourceCreationRequest(
            User user, DataNode dataNode,
            DTO commonCreateDataSourceDTO);

    DTO sendDataSourceUpdateRequest(
            User user, Long centralDataSourceId,
            DTO commonCreateDataSourceDTO);

    ArachnePasswordInfoDTO getPasswordInfo() throws URISyntaxException;

    void registerUserOnCentral(CentralRegisterUserDTO registerUserDTO);

    String loginToCentral(String username, String password);

    User getUserInfoFromCentral(String centralToken);

    JsonResult<CommonProfessionalTypeDTO> getProfessionalTypes();

    JsonResult<CommonUserDTO> getRegisterUser(CommonUserRegistrationDTO dto);

    JsonResult<DTO> getDataSource(User user, Long id);

    JsonResult<List<CommonDataSourceDTO>> getDataSources(User user, List<Long> ids);

    List<CommonUserDTO> suggestUsersFromCentral(User user, String query, Set<String> emails, int limit);

    JsonResult<CommonUserDTO> getUserFromCentral(User user, Long centralUserId);

    void linkUserToDataNodeOnCentral(DataNode dataNode, User user);

    void unlinkUserToDataNodeOnCentral(DataNode dataNode, User user);

    List<User> relinkAllUsersToDataNodeOnCentral(DataNode dataNode, List<User> users);
}
