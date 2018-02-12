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

import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.RequestMapping;
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

/*
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
    }*/

/*    @ApiOperation(value = "Create dataNode entry")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public JsonResult<DataNodeInfoDTO> createDataNode(
            @RequestBody @Valid CreateDataNodeDTO createDataNodeDTO,
            Principal principal
    ) throws AlreadyExistsException, PermissionDeniedException {

        final User user = getAdmin(principal);
        final DataNode dataNode = conversionService.convertAndCheck(createDataNodeDTO, DataNode.class);
        final DataNode saved = dataNodeService.create(user, dataNode);
        final DataNodeInfoDTO savedDTO = conversionService.convertAndCheck(saved, DataNodeInfoDTO.class);
        JsonResult<DataNodeInfoDTO> result = new JsonResult<>(NO_ERROR);
        result.setResult(savedDTO);
        return result;
    }*/

/*    @ApiOperation(value = "Update dataNode")
    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public JsonResult<DataNodeInfoDTO> updateDataNode(
            @RequestBody @Valid CreateDataNodeDTO createDataNodeDTO,
            Principal principal
    ) throws NotExistException, PermissionDeniedException {

        final User user = getAdmin(principal);
        final DataNode dataNode = conversionService.convertAndCheck(createDataNodeDTO, DataNode.class);
        final DataNode updated = dataNodeService.update(user, dataNode);
        final DataNodeInfoDTO updatedDTO = conversionService.convertAndCheck(updated, DataNodeInfoDTO.class);
        JsonResult<DataNodeInfoDTO> result = new JsonResult<>(NO_ERROR);
        result.setResult(updatedDTO);
        return result;
    }*/

}
