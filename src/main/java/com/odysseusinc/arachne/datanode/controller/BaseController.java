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
 * Created: May 22, 2017
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.VALIDATION_ERROR;

import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.UserService;
import java.security.Principal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public abstract class BaseController {

    protected final UserService userService;

    protected BaseController(UserService userService) {

        this.userService = userService;
    }

    protected <T> JsonResult<T> setValidationErrors(BindingResult binding) {

        JsonResult<T> result;
        result = new JsonResult<>(VALIDATION_ERROR);
        for (FieldError fieldError : binding.getFieldErrors()) {
            result.getValidatorErrors().put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return result;
    }

    protected User getUser(Principal principal) throws PermissionDeniedException {

        return userService.getUser(principal);
    }

    protected User getAdmin(Principal principal) throws PermissionDeniedException {

        final User user = userService.getUser(principal);
        if (!user.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase("ROLE_ADMIN"))) {
            throw new PermissionDeniedException("Access denied");
        }
        return user;
    }
}
