package com.odysseusinc.arachne.datanode.service.messaging.prediction;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.messaging.EntityMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class PredictionAtlas2_7Mapper implements EntityMapper<CommonEntity> {

	private static final Logger logger = LoggerFactory.getLogger(PredictionAtlas2_7Mapper.class);
	private static final String PACKAGE_TMPL = "PredictionStudy%d";
	private final AtlasService atlasService;
	private final Template predictionRunnerTemplate;

	public PredictionAtlas2_7Mapper(AtlasService atlasService, Template predictionRunnerTemplate) {

		this.atlasService = atlasService;
		this.predictionRunnerTemplate = predictionRunnerTemplate;
	}

	@Override
	public List<MultipartFile> mapEntity(CommonEntity entity) {

		final Integer localId = entity.getLocalId();
		final String packageName = String.format(PACKAGE_TMPL, localId);
		byte[] data = atlasService.<AtlasClient2_7, byte[]>execute(entity.getOrigin(),
						atlasClient -> atlasClient.getPrediction(localId, packageName));
		List<MultipartFile> files = new ArrayList<>();
		try {
			MultipartFile file = new MockMultipartFile(packageName, packageName + ".zip", MediaType.APPLICATION_OCTET_STREAM_VALUE, data);
			files.add(file);
			files.add(getRunner(packageName, file.getOriginalFilename(), String.format("analysis_%d", localId)));
		} catch (IOException e) {
			logger.error("Failed to build PLP data", e);
			throw new RuntimeIOException("Failed to build PLP data", e);
		}
		return files;
	}

	private MultipartFile getRunner(String packageName, String packageFile, String analysisDir) throws IOException {

		Map<String, Object> params = new HashMap<>();
		params.put("packageName", packageName);
		params.put("packageFile", packageFile);
		params.put("analysisDir", analysisDir);
		String result = predictionRunnerTemplate.apply(params);
		return new MockMultipartFile("runAnalysis.R", result.getBytes());
	}
}
