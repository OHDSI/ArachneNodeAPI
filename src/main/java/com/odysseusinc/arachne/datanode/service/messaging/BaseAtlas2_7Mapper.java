package com.odysseusinc.arachne.datanode.service.messaging;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public abstract class BaseAtlas2_7Mapper implements EntityMapper<CommonEntity> {

	protected static final Logger logger = LoggerFactory.getLogger(BaseAtlas2_7Mapper.class.getName());

	private final AtlasService atlasService;

	protected BaseAtlas2_7Mapper(AtlasService atlasService) {

		this.atlasService = atlasService;
	}

	protected abstract String getPackageName(CommonEntity entity);

	protected abstract Template getRunnerTemplate();

	protected MultipartFile getRunner(String packageName, String packageFile, String analysisDir) throws IOException {

		Map<String, Object> params = new HashMap<>();
		params.put("packageName", packageName);
		params.put("packageFile", packageFile);
		params.put("analysisDir", analysisDir);
		String result = getRunnerTemplate().apply(params);
		return new MockMultipartFile("runAnalysis.R", result.getBytes());
	}

	protected <T extends AtlasClient> List<MultipartFile> doMapping(CommonEntity entity, Function<T, byte[]> requestFunc) {
		final Integer localId = entity.getLocalId();
		final String packageName = getPackageName(entity);
		byte[] data = atlasService.execute(entity.getOrigin(), requestFunc);
		List<MultipartFile> files = new ArrayList<>();
		try {
			String filename = String.format("%s.zip", packageName);
			MultipartFile file = new MockMultipartFile(filename, filename, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);
			files.add(file);
			files.add(getRunner(packageName, file.getName(), String.format("analysis_%d", localId)));
		} catch (IOException e) {
			logger.error("Failed to build analysis data", e);
			throw new RuntimeIOException("Failed to build analysis data", e);
		}
		return files;
	}
}
