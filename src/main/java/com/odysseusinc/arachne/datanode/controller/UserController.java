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

import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.security.RolesConstants;
import com.odysseusinc.arachne.datanode.service.CentralIntegrationService;
import com.odysseusinc.arachne.datanode.service.UserService;
import io.swagger.annotations.ApiOperation;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CentralIntegrationService integrationService;

    @Autowired
    private ModelMapper modelMapper;

    @ApiOperation(value = "Delete given user by login.")
    @RequestMapping(value = "/{login:" + Constants.LOGIN_REGEX + "}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteUser(@PathVariable String login) {

        log.debug("REST request to delete User: {}", login);
        userService.deleteUser(login);
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Disable user by given login.")
    @RequestMapping(value = "/disable/{login:" + Constants.LOGIN_REGEX + "}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(RolesConstants.ROLE_ADMIN)
    public ResponseEntity<Void> disableUser(@PathVariable String login) {

        log.debug("REST request to delete User: {}", login);
        userService.disableUser(login);
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Enable user by given login.")
    @RequestMapping(value = "/enable/{login:" + Constants.LOGIN_REGEX + "}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(RolesConstants.ROLE_ADMIN)
    public ResponseEntity<Void> enableUser(@PathVariable String login) {

        log.debug("REST request to delete User: {}", login);
        userService.enableUser(login);
        return ResponseEntity.ok().build();
    }
}
