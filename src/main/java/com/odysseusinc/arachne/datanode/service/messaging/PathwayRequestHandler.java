package com.odysseusinc.arachne.datanode.service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonPathwayDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.Pathway;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.assertj.core.api.exception.RuntimeIOException;
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

    private static final String DESIGN_FILENAME = "pathways.json";
    private final GenericConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private final CentralSystemClient centralClient;
    private final Template pathwaysRunnerTemplate;
    private static final int PAGE_SIZE = 10000;
    public static final String PATHWAY_BUILD_ERROR = "Failed to build Pathway data";
    public static final Logger LOGGER = LoggerFactory.getLogger(PathwayRequestHandler.class);

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

        List<Pathway> pathways = atlasService.execute(atlasList, c -> c.getPathways(PAGE_SIZE).getContent());
        return pathways.stream()
                .map(pathway -> conversionService.convert(pathway, CommonPathwayDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<MultipartFile> getAtlasObject(String guid) {

        return commonEntityService.findByGuid(guid).map(entity -> {
            Atlas origin = entity.getOrigin();
            JsonNode design = atlasService.execute(origin, atlasClient -> atlasClient.exportPathwayDesign(entity.getLocalId()));
            List<MultipartFile> files = new ArrayList<>();
            try {
							String prettyPrint;
							try {
								ObjectMapper mapper = new ObjectMapper();
								prettyPrint = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(design);
							} catch (JsonProcessingException e) {
								LOGGER.error("JSON Pretty print failed.");
								throw new RuntimeJsonMappingException("JSON Pretty print failed.");
							}
							// Pathways design
							MultipartFile designFile = new MockMultipartFile(DESIGN_FILENAME, DESIGN_FILENAME, MediaType.APPLICATION_JSON_VALUE,
											prettyPrint.getBytes());
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
							files.add(designFile);
							files.add(getRunner(cohortDefinitions));
							return files.stream().filter(Objects::nonNull).collect(Collectors.toList());
						} catch (IOException e) {
            	LOGGER.error(PATHWAY_BUILD_ERROR, e);
            	throw new RuntimeIOException(PATHWAY_BUILD_ERROR, e);
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
				String name = cohort.get("name").textValue();
				cohortDefinitions.add(new CohortDefinition(id, name));
				files.add(getCohortFile(origin, id, String.format("%s.sql", name)));
			});
			return cohortDefinitions;
		}

		private MultipartFile getRunner(List<CohortDefinition> cohorts) throws IOException {

    	Map<String, Object> params = new HashMap<>();
    	String cohortDefinitions = cohorts.stream()
							.map(cd -> String.format("list(file = \"%s.sql\", id = %d)", cd.getName(), cd.getId()))
							.collect(Collectors.joining(","));
    	params.put("cohortDefinitions", cohortDefinitions);
    	String result = pathwaysRunnerTemplate.apply(params);
    	return new MockMultipartFile("main.R", result.getBytes());
		}

		private class CohortDefinition {
    	private Integer id;
    	private String name;

			public CohortDefinition(Integer id, String name) {

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
