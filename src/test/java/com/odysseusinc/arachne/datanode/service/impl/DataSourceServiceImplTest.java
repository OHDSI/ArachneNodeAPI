package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.repository.DataSourceRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DataSourceServiceImplTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private DataSourceRepository dataSourceRepository;
    @InjectMocks
    private DataSourceServiceImpl dataSourceService;

    private final Long dataSourceId = 1000L;
    private final String dataSourceName = "dataSourceName";


    @Test
    public void shouldReturnTrueIfDataSourceWithTheSameNameDoesNotExists() {

        when(dataSourceRepository.countByName(any())).thenReturn(0);

        final boolean isUnique = dataSourceService.isDatasourceNameUnique("dataSourceName", dataSourceId);

        assertThat(isUnique).isTrue();
    }

    @Test
    public void shouldReturnTrueIfDataSourceWithTheSameNameAndIdExists() {

        when(dataSource.getId()).thenReturn(dataSourceId);
        when(dataSourceRepository.countByName(dataSourceName)).thenReturn(1);

        final boolean isUnique = dataSourceService.isDatasourceNameUnique(dataSourceName, dataSourceId);

        assertThat(isUnique).isTrue();
    }

    @Test
    public void shouldReturnFalseIfDataSourceWithTheSameNameButAnotherIdExists() {

        when(dataSource.getId()).thenReturn(dataSourceId);
        when(dataSourceRepository.countByIdNotAndName(33L, dataSourceName)).thenReturn(1);

        final boolean isUnique = dataSourceService.isDatasourceNameUnique(dataSourceName, 33L);

        assertThat(isUnique).isFalse();
    }
}