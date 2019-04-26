package com.odysseusinc.arachne.datanode.service.client.atlas;

import com.fasterxml.jackson.databind.JsonNode;
import com.odysseusinc.arachne.datanode.dto.atlas.EstimationAnalysis;
import com.odysseusinc.arachne.datanode.dto.atlas.PredictionAnalysis;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import org.springframework.http.MediaType;

public interface AtlasClient2_7 extends AtlasClient {
	@RequestLine("GET /prediction")
	List<PredictionAnalysis> getPatientLevelPredictions();

	@RequestLine("GET /prediction/{id}/download?packageName={package}")
	@Headers("Accept: " + MediaType.APPLICATION_OCTET_STREAM_VALUE)
	byte[] getPrediction(@Param("id") Integer id, @Param("package") String packageName);

	@RequestLine("GET /prediction/{id}/export")
	@Headers("Accept: " + MediaType.APPLICATION_JSON_VALUE)
	JsonNode getPrediction(@Param("id") Integer id);

	@RequestLine("GET /estimation")
	List<EstimationAnalysis> getEstimations();

	@RequestLine("GET /estimation/{id}/download?packageName={package}")
	@Headers("Accept: " + MediaType.APPLICATION_OCTET_STREAM_VALUE)
	byte[] getEstimation(@Param("id") Integer id, @Param("package") String packageName);

	@RequestLine("GET /estimation/{id}/export")
	@Headers("Accepts: " + MediaType.APPLICATION_JSON_VALUE)
	JsonNode getEstimation(@Param("id") Integer id);
}
