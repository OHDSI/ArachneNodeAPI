package com.odysseusinc.arachne.datanode.service.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonPathwayDTO;
import com.odysseusinc.arachne.commons.utils.CommonFilenameUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.Pathway;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.util.AtlasUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PathwayRequestHandler extends BaseRequestHandler implements AtlasRequestHandler<CommonPathwayDTO, List<MultipartFile>> {

    private final GenericConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private final CentralSystemClient centralClient;
    private final Template pathwaysRunnerTemplate;
    private static final int PAGE_SIZE = 10000;
    public static final String PATHWAY_BUILD_ERROR = "Failed to build Pathway data";
    public static final Logger LOGGER = LoggerFactory.getLogger(PathwayRequestHandler.class);
    private static final String SKELETON_RESOURCE = "/pathways/hydra/CohortPathways_1.0.1.zip";

    @Autowired
    public PathwayRequestHandler(SqlRenderService sqlRenderService,
																 AtlasService atlasService,
																 CommonEntityService commonEntityService,
																 GenericConversionService conversionService,
																 CentralSystemClient centralClient,
																 Template pathwaysRunnerTemplate) {

        super(sqlRenderService, atlasService);
        this.commonEntityService = commonEntityService;
        this.conversionService = conversionService;
        this.centralClient = centralClient;
			this.pathwaysRunnerTemplate = pathwaysRunnerTemplate;
		}

    @Override
    public List<CommonPathwayDTO> getObjectsList(List<Atlas> atlasList) {

    		List<Atlas> atlases27 = AtlasUtils.filterAtlasByVersion27(atlasList);
        List<Pathway> pathways = atlasService.<AtlasClient2_7, Pathway>execute(atlases27, c -> c.getPathways(PAGE_SIZE).getContent());
        return pathways.stream()
                .map(pathway -> conversionService.convert(pathway, CommonPathwayDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> {
            Atlas origin = entity.getOrigin();
            JsonNode design = atlasService.<AtlasClient2_7, JsonNode>execute(origin, atlasClient -> atlasClient.exportPathwayDesign(entity.getLocalId()));
            String analysisName = design.get("name").asText();
            String packageName = String.format("CohortPathways%d", entity.getLocalId());
            List<MultipartFile> files = new ArrayList<>();
            try {
			 	byte[] packageContent = atlasService.hydrateAnalysis(design, packageName, SKELETON_RESOURCE);
			 	String packageFileName = String.format("CohortPathways - %s.zip", analysisName);
			 	MultipartFile packageFile = new MockMultipartFile(packageFileName, packageFileName, MediaType.APPLICATION_OCTET_STREAM_VALUE,
						packageContent);
				// Target cohorts
				List<CohortDefinition> cohortDefinitions = new ArrayList<>();
				JsonNode targetCohortsNode = design.get("targetCohorts");
				if (targetCohortsNode instanceof ArrayNode) {
					cohortDefinitions.addAll(addCohorts(origin, files, (ArrayNode) targetCohortsNode));
				}
				// Event cohorts
				JsonNode eventCohortsNode = design.get("eventCohorts");
				if (eventCohortsNode instanceof ArrayNode) {
					cohortDefinitions.addAll(addCohorts(origin, files, (ArrayNode) eventCohortsNode));
				}
				int localId = entity.getLocalId();
				files.add(packageFile);
				files.add(getRunner(cohortDefinitions, localId, packageName, String.format("pathwaysAnalysis_%d", localId),
						packageFileName));
				return files.stream().filter(Objects::nonNull).collect(Collectors.toList());
			} catch (IOException e) {
            	LOGGER.error(PATHWAY_BUILD_ERROR, e);
            	throw new UncheckedIOException(PATHWAY_BUILD_ERROR, e);
            }
        }).orElse(null);
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.COHORT_PATHWAY;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

        centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[0]));
    }

    private List<CohortDefinition> addCohorts(Atlas origin, List<MultipartFile> files, ArrayNode cohorts) {

		List<CohortDefinition> cohortDefinitions = new ArrayList<>();
		cohorts.forEach(cohort -> {
				int id = cohort.get("id").intValue();
				String name = CommonFilenameUtils.sanitizeFilename(cohort.get("name").textValue());
				cohortDefinitions.add(new CohortDefinition(id, name));
				files.add(getCohortFile(origin, id, String.format("%s.sql", name)));
			});
			return cohortDefinitions;
	}

	private MultipartFile getRunner(List<CohortDefinition> cohorts, int analysisId, String packageName, String analysisDir, String packageFile) throws IOException {

		Map<String, Object> params = new HashMap<>();
		String cohortDefinitions = cohorts.stream()
							.map(cd -> String.format("list(file = \"%s.sql\", id = %d)", cd.getName(), cd.getId()))
							.collect(Collectors.joining(","));
		params.put("cohortDefinitions", cohortDefinitions);
		params.put("analysisId", analysisId);
		params.put("packageName", packageName);
		params.put("analysisDir", analysisDir);
		params.put("packageFile", packageFile);
		String result = pathwaysRunnerTemplate.apply(params);
		return new MockMultipartFile("file", "main.R", MediaType.TEXT_PLAIN_VALUE, result.getBytes());
		}

		private static class CohortDefinition {
	 	private Integer id;
	 	private String name;

			CohortDefinition(Integer id, String name) {

				this.id = id;
				this.name = name;
			}

			public Integer getId() {

				return id;
			}

			public void setId(Integer id) {

				this.id = id;
			}

			public String getName() {

				return name;
			}

			public void setName(String name) {

				this.name = name;
			}
		}
}
