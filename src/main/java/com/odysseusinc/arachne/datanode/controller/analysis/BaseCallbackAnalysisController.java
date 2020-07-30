/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Jul 8, 2019
 *
 */

package com.odysseusinc.arachne.datanode.controller.analysis;

import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.service.AnalysisService;
import com.odysseusinc.arachne.datanode.service.AnalysisResultsService;
import com.odysseusinc.arachne.datanode.util.AnalysisUtils;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisExecutionStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

public abstract class BaseCallbackAnalysisController {

    public static final String UPDATE_URI = "/api/v1/submissions/{id}/update/{password}";
    public static final String RESULT_URI = "/api/v1/submissions/{id}/result/{password}";

    protected final GenericConversionService conversionService;
    protected final AnalysisService analysisService;
    private final AnalysisResultsService analysisResultsService;

    @Value("${files.store.path}")
    private String filesStorePath;

    @Autowired
    public BaseCallbackAnalysisController(GenericConversionService conversionService,
                                          AnalysisService analysisService,
                                          AnalysisResultsService analysisResultsService) {

        this.conversionService = conversionService;
        this.analysisService = analysisService;
        this.analysisResultsService = analysisResultsService;
    }

    @PostMapping(value = UPDATE_URI, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void updateSubmission(@PathVariable Long id,
                                 @PathVariable String password,
                                 @RequestBody AnalysisExecutionStatusDTO status
    ) {

        if (!id.equals(status.getId())) {
            String exceptionMessage = String.format("Path variable id='%s' not equal status.id='%s'", id, status.getId());
            throw new IllegalArgumentException(exceptionMessage);
        }
        doUpdateSubmission(status, password);
    }

    protected Optional<Analysis> doUpdateSubmission(AnalysisExecutionStatusDTO status, String password) {

        Long id = status.getId();
        String stdout = status.getStdout();
        return analysisService.updateStatus(id, stdout, password);
    }

    @PostMapping(value = RESULT_URI, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void analysisResult(@PathVariable Long id,
                               @PathVariable String password,
                               @RequestPart("analysisResult") AnalysisResultDTO result,
                               @RequestPart("file") MultipartFile[] files
    ) throws IOException {

        if (!id.equals(result.getId())) {
            String exceptionMessage = String.format("Path variable id='%s' not equal status.id='%s'", id, result.getId());
            throw new IllegalArgumentException(exceptionMessage);
        }
        File resultDir = AnalysisUtils.storeMultipartFiles(filesStorePath, files);
        doSaveResults(result, resultDir);
    }

    protected Analysis doSaveResults(AnalysisResultDTO result, File resultDir) {

        Analysis analysis = conversionService.convert(result, Analysis.class);
        return analysisResultsService.saveResults(analysis, resultDir);
    }

}
