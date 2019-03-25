package com.odysseusinc.arachne.datanode.service.client.atlas;

import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionInfo;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import java.util.Map;

public interface AtlasClient2_5 extends AtlasClient {
	@RequestLine("GET /plp")
	List<PatientLevelPredictionInfo> getPatientLevelPredictions();

	@RequestLine("GET /plp/{id}")
	Map<String, Object> getPatientLevelPrediction(@Param("id") Integer id);
}
