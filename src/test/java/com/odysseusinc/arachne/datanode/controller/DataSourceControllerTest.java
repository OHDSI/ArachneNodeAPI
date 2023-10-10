package com.odysseusinc.arachne.datanode.controller;

import com.odysseusinc.arachne.commons.api.v1.dto.util.JsonResult;
import com.odysseusinc.arachne.datanode.dto.datasource.CreateDataSourceDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.Role;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import com.odysseusinc.arachne.datanode.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DataSourceControllerTest {

    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private Principal principal;
    @Mock
    private MultipartFile multipartFile;
    @Mock
    private CreateDataSourceDTO createDataSourceDTO;
    @Mock
    private DataSourceService dataSourceService;
    @Mock
    private UserService userService;
    @Mock
    private GenericConversionService conversionService;
    @Mock
    private User user;
    @Mock
    private DataSource dataSource;
    @InjectMocks
    private DataSourceController dataSourceController;

    private final Long dataSourceId = 1000L;
    private final FieldError fieldError = new FieldError("object", "fieldName", "field error message");

    @Test
    public void shouldFailValidationAndReturnFieldValidationError() {

        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError));

        final JsonResult creationResults = dataSourceController.add(principal, createDataSourceDTO, multipartFile, bindingResult);

        assertThat(creationResults.getErrorCode()).isEqualTo(JsonResult.ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(creationResults.getValidatorErrors()).containsOnlyKeys("fieldName");
        verify(dataSourceService, never()).create(any(), any());
    }


    @Test
    public void shouldFailValidationAndReturnAlreadyExistingDatasourceNameError() {

        when(bindingResult.hasErrors()).thenReturn(false);
        when(dataSourceService.isDatasourceNameUnique(any(), any())).thenReturn(false);

        final JsonResult creationResults = dataSourceController.update(principal, createDataSourceDTO, dataSourceId, multipartFile, bindingResult);

        assertThat(creationResults.getErrorCode()).isEqualTo(JsonResult.ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(creationResults.getValidatorErrors()).containsOnlyKeys("name");
    }

    @Test
    public void shouldPassValidationAndSaveNewDatasource() {
        when(userService.getUser(principal)).thenReturn(user);

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        when(user.getRoles()).thenReturn(Arrays.asList(adminRole));
        when(conversionService.convert(createDataSourceDTO, DataSource.class)).thenReturn(dataSource);

        when(bindingResult.hasErrors()).thenReturn(false);
        when(dataSourceService.isDatasourceNameUnique(any(), any())).thenReturn(true);

        final JsonResult creationResults = dataSourceController.add(principal, createDataSourceDTO, multipartFile, bindingResult);

        assertThat(creationResults.getErrorCode()).isEqualTo(JsonResult.ErrorCode.NO_ERROR.getCode());
        verify(dataSourceService).create(any(), any());
    }

    @Test
    public void shouldPassValidationAndUpdateDatasource() {
        when(userService.getUser(principal)).thenReturn(user);

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        when(user.getRoles()).thenReturn(Arrays.asList(adminRole));
        when(conversionService.convert(createDataSourceDTO, DataSource.class)).thenReturn(dataSource);

        when(bindingResult.hasErrors()).thenReturn(false);
        when(dataSourceService.isDatasourceNameUnique(any(), any())).thenReturn(true);

        final JsonResult creationResults = dataSourceController.update(principal, createDataSourceDTO, dataSourceId, multipartFile, bindingResult);

        assertThat(creationResults.getErrorCode()).isEqualTo(JsonResult.ErrorCode.NO_ERROR.getCode());
        verify(dataSourceService).update(any(), any());
        verify(dataSource).setId(dataSourceId);
    }
}