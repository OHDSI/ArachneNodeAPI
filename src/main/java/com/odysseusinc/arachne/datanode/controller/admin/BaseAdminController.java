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
 * Created: October 06, 2017
 *
 */

package com.odysseusinc.arachne.datanode.controller.admin;

import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.commons.utils.UserIdUtils;
import com.odysseusinc.arachne.datanode.dto.user.UserDTO;
import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.UserService;
import io.swagger.annotations.ApiOperation;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class BaseAdminController {

    public static final int SUGGEST_LIMIT = 10;

    protected UserService userService;
    protected GenericConversionService conversionService;
    protected AtlasService atlasService;

    @Autowired
    public BaseAdminController(
            UserService userService,
            GenericConversionService conversionService,
            AtlasService atlasService
    ) {
        this.userService = userService;
        this.conversionService = conversionService;
        this.atlasService = atlasService;
    }

    @ApiOperation(value = "Get all admins", hidden = true)
    @RequestMapping(value = "/api/v1/admin/admins", method = RequestMethod.GET)
    public JsonResult<List<UserDTO>> getAdmins(
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortAsc", required = false) Boolean sortAsc
    ) throws PermissionDeniedException {

        JsonResult<List<UserDTO>> result;
        List<User> users = userService.getAllAdmins(sortBy, sortAsc);
        List<UserDTO> dtos = users.stream()
                .map(user -> conversionService.convert(user, UserDTO.class))
                .collect(Collectors.toList());
        result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        result.setResult(dtos);
        return result;
    }

    @ApiOperation("Suggests user according to query to add admin")
    @RequestMapping(value = "/api/v1/admin/admins/suggest", method = RequestMethod.GET)
    public JsonResult<List<UserDTO>> suggestAdmins(
            Principal principal,
            @RequestParam("query") String query,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {

        JsonResult<List<UserDTO>> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        userService
                .findByUsername(principal.getName())
                .ifPresent(user -> {
                    List<User> users = userService.suggestNotAdmin(user, query, limit == null ? SUGGEST_LIMIT : limit);
                    result.setResult(users.stream().map(u -> conversionService
                            .convert(u, UserDTO.class))
                            .collect(Collectors.toList())
                    );
                });
        return result;
    }

    @ApiOperation("Remove admin")
    @RequestMapping(value = "/api/v1/admin/admins/{uuid}", method = RequestMethod.DELETE)
    public JsonResult removeAdmin(@PathVariable String uuid) {

        userService.remove(UserIdUtils.uuidToId(uuid));
        return new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
    }

    @ApiOperation("Add admin from central")
    @RequestMapping(value = "/api/v1/admin/admins/{uuid}", method = RequestMethod.POST)
    public JsonResult addAdminFromCentral(
            Principal principal,
            @PathVariable String uuid) {

        JsonResult<UserDTO> result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        userService
                .findByUsername(principal.getName())
                .ifPresent(loginedUser -> {
                    final User user = userService.addUserFromCentral(loginedUser, UserIdUtils.uuidToId(uuid));
                    result.setResult(conversionService.convert(user, UserDTO.class));
                });
        return result;
    }

    @ApiOperation("Check Atlas Connection")
    @RequestMapping(value = "/api/v1/admin/atlases/{id}/connection", method = RequestMethod.POST)
    public JsonResult checkAtlasConnection(@PathVariable("id") Long id) {

        JsonResult result = new JsonResult<>(JsonResult.ErrorCode.NO_ERROR);
        Atlas atlas = atlasService.getById(id);
        String atlasVersion = atlasService.checkConnection(atlas);
        result.setErrorMessage("Atlas version: " + atlasVersion);
        return result;
    }

}
