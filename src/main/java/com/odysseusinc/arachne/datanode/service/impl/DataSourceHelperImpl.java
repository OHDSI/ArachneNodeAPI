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
 * Created: July 11, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.service.CohortService;
import com.odysseusinc.arachne.datanode.service.DataSourceHelper;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.ohdsi.sql.SqlTranslate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;

@Service
public class DataSourceHelperImpl implements DataSourceHelper {

    public static final String FILE_FOR_PING_REQUEST = "test.sql";

    // SqlRender requires semicolon in the end of a query to do proper translation
    public static final String PING_REQUEST = "select 1;";
    private final GenericConversionService conversionService;
    private final CohortService cohortService;

    private final String baseNodeUrl;

    @Autowired
    public DataSourceHelperImpl(
            GenericConversionService conversionService,
            CohortService cohortService,
            @Value("${datanode.baseURL}") String datanodeBaseURL,
            @Value("${datanode.port}") String datanodePort
    ) {

        this.conversionService = conversionService;
        this.cohortService = cohortService;
        this.baseNodeUrl = String.format("%s:%s", datanodeBaseURL, datanodePort);
    }

    @Override
    public AnalysisRequestDTO getAnalysisRequestDTO(DataSource dataSource, Path tempDirectory, Long id, String path) throws IOException {

        AnalysisRequestDTO requestDTO = getDataSourceCheckRequest(dataSource, tempDirectory);
        requestDTO.setResultCallback(String.format("%s%s", baseNodeUrl, path));
        requestDTO.setId(id);
        return requestDTO;
    }

    private AnalysisRequestDTO getDataSourceCheckRequest(DataSource dataSource, Path tempDirectory) throws IOException {

        AnalysisRequestDTO request = conversionService.convert(dataSource, AnalysisRequestDTO.class);
        final Path testFile = Paths.get(tempDirectory.toAbsolutePath().toString(), FILE_FOR_PING_REQUEST);
        String testSql = cohortService.translateSql(
                dataSource.getType().getOhdsiDB(),
                SqlTranslate.generateSessionId(),
                dataSource.getResultSchema(),
                PING_REQUEST
        );
        FileUtils.write(testFile.toFile(), testSql, Charset.defaultCharset());
        return request;
    }
}