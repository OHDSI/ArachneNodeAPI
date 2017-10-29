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
 * Created: December 19, 2016
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.NO_ERROR;

import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.dto.datanode.CreateDataNodeDTO;
import com.odysseusinc.arachne.datanode.dto.datanode.DataNodeInfoDTO;
import com.odysseusinc.arachne.datanode.exception.AlreadyExistsException;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.UserService;
import io.swagger.annotations.ApiOperation;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/datanode")
public class DataNodeController extends BaseController {

    private final DataNodeService dataNodeService;
    private final ConversionService conversionService;

    @Autowired
    public DataNodeController(UserService userService,
                              DataNodeService dataNodeService,
                              ConversionService conversionService
    ) {

        super(userService);
        this.dataNodeService = dataNodeService;
        this.conversionService = conversionService;
    }


    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "get current DataNode")
    public JsonResult<DataNodeInfoDTO> getDataNodeInfo() throws NotExistException {


        Optional<DataNode> currentDataNode = dataNodeService.findCurrentDataNode();
        currentDataNode.orElseThrow(() ->
                new NotExistException("DataNode is not registered, please register", DataNode.class));
        DataNode datanode = currentDataNode.get();
        DataNodeInfoDTO dataNodeInfoDTO = conversionService.convert(datanode, DataNodeInfoDTO.class);
        JsonResult<DataNodeInfoDTO> result = new JsonResult<>(NO_ERROR);
        result.setResult(dataNodeInfoDTO);
        return result;
    }

    @ApiOperation(value = "Create dataNode entry")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public JsonResult<DataNodeInfoDTO> createDataNode(
            @RequestBody CreateDataNodeDTO createDataNodeDTO,
            Principal principal
    ) throws AlreadyExistsException, PermissionDeniedException {

        final User user = getAdmin(principal);
        final DataNode dataNode = conversionService.convert(createDataNodeDTO, DataNode.class);
        final List<User> users = userService.getAllUsers(null, null);
        final DataNode saved = dataNodeService.create(user, dataNode, users);
        final DataNodeInfoDTO savedDTO = conversionService.convert(saved, DataNodeInfoDTO.class);
        JsonResult<DataNodeInfoDTO> result = new JsonResult<>(NO_ERROR);
        result.setResult(savedDTO);
        return result;
    }

    @ApiOperation(value = "Update dataNode")
    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public JsonResult<DataNodeInfoDTO> updateDataNode(
            @RequestBody CreateDataNodeDTO createDataNodeDTO,
            Principal principal
    ) throws NotExistException, PermissionDeniedException {

        final User user = getAdmin(principal);
        final DataNode dataNode = conversionService.convert(createDataNodeDTO, DataNode.class);
        final DataNode updated = dataNodeService.update(user, dataNode);
        final DataNodeInfoDTO updatedDTO = conversionService.convert(updated, DataNodeInfoDTO.class);
        JsonResult<DataNodeInfoDTO> result = new JsonResult<>(NO_ERROR);
        result.setResult(updatedDTO);
        return result;
    }

}
