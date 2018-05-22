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
 * Created: November 18, 2016
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.SYSTEM_ERROR;
import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.UNAUTHORIZED;
import static com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult.ErrorCode.VALIDATION_ERROR;

import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.exception.AuthException;
import com.odysseusinc.arachne.datanode.exception.IllegalOperationException;
import com.odysseusinc.arachne.datanode.exception.IntegrationValidationException;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.exception.ValidationException;
import com.odysseusinc.arachne.datanode.service.UserService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

@ControllerAdvice
public class ExceptionHandlingAdvice extends BaseController {

    private static final String ERROR_MESSAGE_WITH_TOKEN = "Please contact system administrator and provide this error token: %s";
    private static final String ERROR_MESSAGE = "An error has occurred. Please contact system administrator";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlingAdvice.class);
    @Value("${datanode.app.errorsTokenEnabled}")
    private boolean errorsTokenEnabled;

    private static final String STATIC_CONTENT_FOLDER = "public";
    private static final String INDEX_FILE = STATIC_CONTENT_FOLDER + "/index.html";

    private WebApplicationContext webApplicationContext;

    public ExceptionHandlingAdvice(UserService userService, WebApplicationContext webApplicationContext) {

        super(userService);
        this.webApplicationContext = webApplicationContext;
    }

    @ExceptionHandler({SQLException.class, DataAccessException.class})
    public ResponseEntity handleDataAccessExceptions(Exception ex) {

        return getErrorResponse(SYSTEM_ERROR, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonResult> exceptionHandler(Exception ex) {

        return getErrorResponse(SYSTEM_ERROR, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JsonResult> exceptionHandler(MethodArgumentNotValidException ex) {

        JsonResult result = new JsonResult<>(VALIDATION_ERROR);
        if (ex.getBindingResult().hasErrors()) {
            result = setValidationErrors(ex.getBindingResult());
        }
        return getErrorResponse(result, ex);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<JsonResult> exceptionHandler(IOException ex) {

        return getErrorResponse(SYSTEM_ERROR, ex);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<JsonResult> exceptionHandler(AuthException ex) {

        return getErrorResponse(UNAUTHORIZED, ex);
    }

    private ResponseEntity<JsonResult> getErrorResponse(JsonResult.ErrorCode errorCode, Exception ex) {

        JsonResult result = new JsonResult<>(errorCode);
        return getErrorResponse(result, ex);
    }

    private ResponseEntity<JsonResult> getErrorResponse(final JsonResult result, final Exception ex) {

        final String message = getErrorMessage(result, ex);
        result.setErrorMessage(message);

        if (errorsTokenEnabled) {
            final String errorToken = generateErrorToken();
            LOGGER.error(message + " token: " + errorToken, ex);
            result.setErrorMessage(String.format(ERROR_MESSAGE_WITH_TOKEN, errorToken));
        } else {
            LOGGER.error(message, ex);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private String getErrorMessage(final JsonResult result, final Exception ex) {

        // return default ERROR_MESSAGE for all exceptions except Validation error
        return Objects.equals(result.getErrorCode(), VALIDATION_ERROR.getCode()) ? ex.getMessage() : ERROR_MESSAGE;
    }

    @ExceptionHandler(IntegrationValidationException.class)
    public ResponseEntity<JsonResult> exceptionHandler(IntegrationValidationException ex) {

        LOGGER.error(ex.getMessage(), ex);
        return new ResponseEntity<>(ex.getJsonResult(), HttpStatus.OK);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<JsonResult> exceptionHandler(ValidationException ex) {

        return getErrorResponse(VALIDATION_ERROR, ex);
    }

    @ExceptionHandler(NotExistException.class)
    public ResponseEntity<JsonResult> exceptionHandler(NotExistException ex) {

        return getErrorResponse(SYSTEM_ERROR, ex);
    }

    @ExceptionHandler(IllegalOperationException.class)
    public ResponseEntity<JsonResult> exceptionHandler(IllegalOperationException ex) {

        return getErrorResponse(SYSTEM_ERROR, ex);
    }

    private String generateErrorToken() {

        return UUID.randomUUID().toString();
    }

    @ExceptionHandler({NoHandlerFoundException.class})
    public void handleNotFoundError(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler() {
            @Override
            protected Resource getResource(HttpServletRequest request) throws IOException {

                String requestPath = request.getRequestURI().substring(request.getContextPath().length());

                ClassPathResource resource = new ClassPathResource(STATIC_CONTENT_FOLDER + requestPath);
                if (!resource.exists()) {
                    resource = new ClassPathResource(INDEX_FILE);
                }

                return resource;
            }
        };

        handler.setServletContext(webApplicationContext.getServletContext());
        handler.setLocations(Collections.singletonList(new ClassPathResource("classpath:/" + STATIC_CONTENT_FOLDER + "/")));
        handler.afterPropertiesSet();

        handler.handleRequest(request, response);
    }
}
