package com.odysseusinc.arachne.datanode.service.messaging.prediction;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.datanode.dto.atlas.PatientLevelPredictionInfo;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_5;
import com.odysseusinc.arachne.datanode.service.messaging.BaseRequestHandler;
import com.odysseusinc.arachne.datanode.service.messaging.EntityMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class PredictionAtlas2_5Mapper extends BaseRequestHandler implements EntityMapper<PatientLevelPredictionInfo, CommonEntity, AtlasClient2_5> {

	private static final Logger logger = LoggerFactory.getLogger(PredictionAtlas2_5Mapper.class);
	private Template patientLevelPredictionRunnerTemplate;

	private static final String INITIAL_SUFFIX = " initial population.sql";
	private static final String OUTCOME_SUFFIX = " outcome population.sql";

	public PredictionAtlas2_5Mapper(SqlRenderService sqlRenderService, AtlasService atlasService, Template patientLevelPredictionRunnerTemplate) {

		super(sqlRenderService, atlasService);
		this.patientLevelPredictionRunnerTemplate = patientLevelPredictionRunnerTemplate;
	}

	@Override
	public List<PatientLevelPredictionInfo> getEntityList(AtlasClient2_5 client) {

		return client.getPatientLevelPredictions();
	}

	@Override
	public List<MultipartFile> mapEntity(CommonEntity entity) {

		Map<String, Object> info = atlasService.<AtlasClient2_5, Map<String, Object>>execute(
						entity.getOrigin(),
						atlasClient -> atlasClient.getPatientLevelPrediction(entity.getLocalId())
		);
		List<MultipartFile> files = new ArrayList<>(6);
		try {
			String initialName = info.get("name") + INITIAL_SUFFIX;
			String outcomeName = info.get("name") + OUTCOME_SUFFIX;

			files.add(getAnalysisDescription(info));
			files.add(getCohortFile(entity.getOrigin(), (Integer) info.get("treatmentId"), initialName));
			files.add(getCohortFile(entity.getOrigin(), (Integer) info.get("outcomeId"), outcomeName));
			files.add(getRunner(initialName, outcomeName));
		}catch (IOException e){
			logger.error("Failed to build PLP data", e);
			throw new RuntimeIOException("Failed to build PLP data", e);
		}
		return files;
	}

	private MultipartFile getRunner(String initialName, String outcomeName) throws IOException {

		Map<String, Object> params = new HashMap<>();
		params.put("initialFileName", initialName);
		params.put("outcomeFileName", outcomeName);
		String result = patientLevelPredictionRunnerTemplate.apply(params);
		return new MockMultipartFile("main.r", result.getBytes());
	}

}
