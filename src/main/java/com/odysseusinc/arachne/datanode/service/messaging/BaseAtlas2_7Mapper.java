package com.odysseusinc.arachne.datanode.service.messaging;

import static com.odysseusinc.arachne.commons.utils.CommonFileUtils.ANALYSIS_INFO_FILE_DESCRIPTION;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.utils.AnalysisArchiveUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.exception.ArachneSystemRuntimeException;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AnalysisInfoBuilder;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.ohdsi.hydra.Hydra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public abstract class BaseAtlas2_7Mapper<T extends BaseAtlasEntity> implements EntityMapper<T, CommonEntity, AtlasClient2_7> {

	protected static final Logger logger = LoggerFactory.getLogger(BaseAtlas2_7Mapper.class.getName());
	private static final String RUN_ANALYSIS_FILE = "runAnalysis.R";

	private final AtlasService atlasService;
	private final AnalysisInfoBuilder analysisInfoBuilder;

	protected BaseAtlas2_7Mapper(AtlasService atlasService, AnalysisInfoBuilder analysisInfoBuilder) {

		this.atlasService = atlasService;
		this.analysisInfoBuilder = analysisInfoBuilder;
	}

	protected abstract String getPackageName(CommonEntity entity);

	protected abstract Template getRunnerTemplate();

	protected MultipartFile getRunner(String packageName, String packageFile, String analysisDir) throws IOException {

		Map<String, Object> params = new HashMap<>();
		params.put("packageName", packageName);
		params.put("packageFile", packageFile);
		params.put("analysisDir", analysisDir);
		String result = getRunnerTemplate().apply(params);
		return new MockMultipartFile(RUN_ANALYSIS_FILE, RUN_ANALYSIS_FILE, "plain/text", result.getBytes());
	}

	protected <T extends AtlasClient> List<MultipartFile> doMapping(CommonEntity entity, Function<T, JsonNode> requestFunc) {
		final Integer localId = entity.getLocalId();
		final String packageName = getPackageName(entity);
		JsonNode analysis = atlasService.execute(entity.getOrigin(), requestFunc);
		((ObjectNode)analysis).put("packageName", packageName);
		try {
			List<MultipartFile> files = new ArrayList<>();
			String filename = AnalysisArchiveUtils.getArchiveFileName(entity.getAnalysisType(), AnalysisArchiveUtils.getAnalysisName(analysis));
			byte[] data = hydrate(analysis);
			String description = analysisInfoBuilder.generatePredictionAnalysisDescription(analysis);
			MultipartFile file = new MockMultipartFile(filename, filename, MediaType.APPLICATION_OCTET_STREAM_VALUE, data);
			files.add(file);
			files.add(getRunner(packageName, file.getName(), String.format("analysis_%d", localId)));
			files.add(new MockMultipartFile("file", ANALYSIS_INFO_FILE_DESCRIPTION, MediaType.TEXT_PLAIN_VALUE, description.getBytes()));
			return files;
		} catch (IOException e) {
			throw new ArachneSystemRuntimeException("Failed to build analysis data", e);
		}
	}

	protected byte[] hydrate(JsonNode analysis) throws IOException {

		Hydra hydra = new Hydra(analysis.toString());
		byte[] data;
		try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			hydra.hydrate(out);
			data = out.toByteArray();
		}
		return data;
	}
}
