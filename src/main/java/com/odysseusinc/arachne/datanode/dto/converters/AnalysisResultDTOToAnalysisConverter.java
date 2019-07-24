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

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.datanode.controller.analysis.BaseCallbackAnalysisController;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisState;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisStateEntry;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultDTO;
import java.util.Date;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class AnalysisResultDTOToAnalysisConverter implements Converter<AnalysisResultDTO, Analysis>, InitializingBean {

    private GenericConversionService conversionService;
    @Value("${datanode.baseURL}")
    private String datanodeBaseURL;
    @Value("${datanode.port}")
    private String datanodePort;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public AnalysisResultDTOToAnalysisConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public Analysis convert(AnalysisResultDTO analysisResult) {

        Analysis analysis = new Analysis();
        analysis.setId(analysisResult.getId());
        analysis.setStatus(analysisResult.getStatus());
        analysis.setStdout(analysisResult.getStdout());
        AnalysisStateEntry stateEntry = new AnalysisStateEntry(new Date(),
                AnalysisState.EXECUTED,
                "Received analysisResult from Execution Engine", analysis);
        analysis.getStateHistory().add(stateEntry);

        String updateStatusCallback = String.format(
                "%s:%s%s",
                datanodeBaseURL,
                datanodePort,
                BaseCallbackAnalysisController.UPDATE_URI
        );
        String resultCallback = String.format(
                "%s:%s%s",
                datanodeBaseURL,
                datanodePort,
                BaseCallbackAnalysisController.RESULT_URI
        );
        analysis.setUpdateStatusCallback(updateStatusCallback);
        analysis.setResultCallback(resultCallback);


        return analysis;
    }
}
