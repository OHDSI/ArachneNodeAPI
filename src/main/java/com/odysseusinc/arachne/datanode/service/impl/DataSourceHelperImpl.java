/**
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
import com.odysseusinc.arachne.datanode.service.DataSourceHelper;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;

@Service
public class DataSourceHelperImpl implements DataSourceHelper {

    private final GenericConversionService conversionService;

    @Autowired
    public DataSourceHelperImpl(GenericConversionService conversionService) {

        this.conversionService = conversionService;
    }

    @Override
    public AnalysisRequestDTO getDataSourceCheckRequest(DataSource dataSource, Path tempDirectory) throws IOException {

        AnalysisRequestDTO request = conversionService.convert(dataSource, AnalysisRequestDTO.class);
        final Path testFile = Paths.get(tempDirectory.toAbsolutePath().toString(), "test.sql");
        FileUtils.write(testFile.toFile(), "select 1", Charset.defaultCharset());
        return request;
    }
}
