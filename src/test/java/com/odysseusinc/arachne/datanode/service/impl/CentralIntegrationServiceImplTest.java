package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonLinkUserToDataNodeDTO;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.util.CentralUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.GenericConversionService;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CentralIntegrationServiceImplTest {

    @Mock
    private GenericConversionService conversionService;
    @Mock
    private CentralUtil centralUtil;
    @Mock
    private CentralClient centralClient;
    @Mock
    private CentralSystemClient centralSystemClient;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private DataNodeService dataNodeService;
    @Mock
    private DataNode dataNode;
    @Mock
    private CommonLinkUserToDataNodeDTO dto;
    private User user;

    
    @InjectMocks
    private CentralIntegrationServiceImpl centralIntegrationService;

    @BeforeEach
    public void setUp() {
        user = new User();
        when(conversionService.convert(user,CommonLinkUserToDataNodeDTO.class)).thenReturn(dto);
        centralIntegrationService.centralSystemClient = centralSystemClient;
    }

    @Test
    public void shouldRelinkAndEnableUser() {

        user.setSync(false);
        centralIntegrationService.relinkUsersToDataNodeOnCentral(dataNode, Arrays.asList(user));

        assertThat(user.getSync()).isTrue();
    }

    @Test
    public void shouldDisableUserWhichCannotBeRelinked() {

        doThrow(new RuntimeException()).when(centralSystemClient).linkUser(any(), any());
        user.setSync(false);
        centralIntegrationService.relinkUsersToDataNodeOnCentral(dataNode, Arrays.asList(user));

        assertThat(user.getSync()).isFalse();
        assertThat(user.getEnabled()).isFalse();
    }

}
