package com.odysseusinc.arachne.datanode.service.client.engine;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface EngineClient {

    @RequestLine("POST /api/v1/analyze")
    @Headers({
            "Content-Type: " + MediaType.MULTIPART_FORM_DATA_VALUE,
            "arachne-compressed: true",
            "arachne-waiting-compressed-result: {compressed}",
            "arachne-datasource-check: {healthCheck}"
    })
    AnalysisRequestStatusDTO sendAnalysisRequest(@Param("analysisRequest") AnalysisRequestDTO analysisRequest,
                                                 @Param("file") MultipartFile file,
                                                 @Param("compressed") Boolean compressedResult,
                                                 @Param("healthCheck") Boolean healthCheck);
}
