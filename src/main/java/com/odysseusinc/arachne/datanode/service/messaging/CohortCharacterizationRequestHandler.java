package com.odysseusinc.arachne.datanode.service.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCcShortDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortCharacterization;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.util.AtlasUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.exception.RuntimeIOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CohortCharacterizationRequestHandler implements AtlasRequestHandler<CommonEntityDTO, List<MultipartFile>> {

	private static final int PAGE_SIZE = 10000;
	private static final String PACKAGE_NAME = "CohortCharacterization%d";
	private static final String SKELETON_RESOURCE = "/cc/hydra/CohortCharacterization_v0.0.1.zip";
	private final AtlasService atlasService;
	private final GenericConversionService conversionService;
	private final CommonEntityService commonEntityService;
	private final Template runnerTemplate;
	private final CentralSystemClient centralClient;

	public CohortCharacterizationRequestHandler(AtlasService atlasService,
																							GenericConversionService conversionService,
																							CommonEntityService commonEntityService,
																							@Qualifier("cohortCharacterizationTemplate") Template runnerTemplate,
																							CentralSystemClient centralClient) {

		this.atlasService = atlasService;
		this.conversionService = conversionService;
		this.commonEntityService = commonEntityService;
		this.runnerTemplate = runnerTemplate;
		this.centralClient = centralClient;
	}

	@Override
	public List<CommonEntityDTO> getObjectsList(List<Atlas> atlasList) {

		List<Atlas> atlases27 = AtlasUtils.filterAtlasByVersion27(atlasList);
		List<CohortCharacterization> ccList = atlasService.<AtlasClient2_7, CohortCharacterization>execute(atlases27,
						c -> c.getCohortCharacterizations(PAGE_SIZE).getContent());
		return ccList.stream()
						.map(cc -> conversionService.convert(cc, CommonCcShortDTO.class))
						.collect(Collectors.toList());
	}

	@Override
	public List<MultipartFile> getAtlasObject(String guid) {

		return commonEntityService.findByGuid(guid).map(entity -> {
			Integer localId = entity.getLocalId();
			List<MultipartFile> files = new ArrayList<>();
			String packageName = String.format(PACKAGE_NAME, localId);

			JsonNode analysis = atlasService.<AtlasClient2_7, JsonNode>execute(entity.getOrigin(), c -> c.getCohortCharacterization(entity.getLocalId()));

			try {
				byte[] ccPackage = atlasService.hydrateAnalysis(analysis, packageName, SKELETON_RESOURCE);
				String filename = packageName + ".zip";
				MultipartFile file = new MockMultipartFile(filename, filename, MediaType.APPLICATION_OCTET_STREAM_VALUE, ccPackage);
				files.add(file);
				files.add(getRunner(packageName, file.getName(), String.format("analysis_%d", localId), localId));
			} catch (IOException e) {
				throw new RuntimeIOException("Failed to build analysis data", e);
			}
			return files;
		}).orElse(null);
	}

	@Override
	public CommonAnalysisType getAnalysisType() {

		return CommonAnalysisType.COHORT_CHARACTERIZATION;
	}

	@Override
	public void sendResponse(List<MultipartFile> response, String id) {

		centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[0]));
	}

	protected MultipartFile getRunner(String packageName, String packageFile, String analysisDir, int analysisId) throws IOException {

		Map<String, Object> params = new HashMap<>();
		params.put("packageName", packageName);
		params.put("packageFile", packageFile);
		params.put("analysisDir", analysisDir);
		params.put("analysisId", analysisId);
		String result = runnerTemplate.apply(params);
		return new MockMultipartFile("runAnalysis.R", result.getBytes());
	}

}
