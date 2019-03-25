package com.odysseusinc.arachne.datanode.service.client.atlas;

import com.odysseusinc.arachne.datanode.dto.atlas.EstimationListItem;
import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionInfo;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import org.springframework.http.MediaType;

public interface AtlasClient2_7 extends AtlasClient {
	@RequestLine("GET /prediction")
	List<PatientLevelPredictionInfo> getPatientLevelPredictions();

	@RequestLine("GET /prediction/{id}/download?packageName={package}")
	@Headers("Accept: " + MediaType.APPLICATION_OCTET_STREAM_VALUE)
	byte[] getPrediction(@Param("id") Integer id, @Param("package") String packageName);

	@RequestLine("GET /estimation")
	List<EstimationListItem> getEstimations();

	@RequestLine("GET /estimation/{id}/download?packageName={package}")
	@Headers("Accept: " + MediaType.APPLICATION_OCTET_STREAM_VALUE)
	byte[] getEstimation(@Param("id") Integer id, @Param("package") String packageName);
}
