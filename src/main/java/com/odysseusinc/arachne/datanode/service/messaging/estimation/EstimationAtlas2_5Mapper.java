package com.odysseusinc.arachne.datanode.service.messaging.estimation;

import static com.odysseusinc.arachne.datanode.service.messaging.MessagingUtils.ignorePreprocessingMark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.commons.utils.CommonFileUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.dto.atlas.ComparativeCohortAnalysis;
import com.odysseusinc.arachne.datanode.dto.atlas.ComparativeCohortAnalysisInfo;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_5;
import com.odysseusinc.arachne.datanode.service.messaging.BaseRequestHandler;
import com.odysseusinc.arachne.datanode.service.messaging.EntityMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.io.IOUtils;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class EstimationAtlas2_5Mapper extends BaseRequestHandler implements EntityMapper<ComparativeCohortAnalysis, CommonEntity, AtlasClient2_5> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EstimationAtlas2_5Mapper.class);

	private final CohortExpressionQueryBuilder queryBuilder;

	private final Template runnerTemplate;

	public EstimationAtlas2_5Mapper(SqlRenderService sqlRenderService, AtlasService atlasService, CohortExpressionQueryBuilder queryBuilder, Template runnerTemplate) {

		super(sqlRenderService, atlasService);
		this.queryBuilder = queryBuilder;
		this.runnerTemplate = runnerTemplate;
	}

	@Override
	public List<ComparativeCohortAnalysis> getEntityList(AtlasClient2_5 client) {

		return client.getEstimations();
	}

	@Override
	public List<MultipartFile> mapEntity(CommonEntity entity) {

		final List<MultipartFile> result = new ArrayList<>();

		ComparativeCohortAnalysisInfo analysis = atlasService.execute(
						entity.getOrigin(),
						atlasClient -> ((AtlasClient2_5)atlasClient).getEstimation(entity.getLocalId())
		);
		if (analysis != null) {
			try {
				String name = analysis.getName().trim();

				String estimationJson = buildEstimationDesign(analysis);
				result.add(new MockMultipartFile("file", getEstimationFilename(name), MediaType.TEXT_PLAIN_VALUE, estimationJson.getBytes()));

				String targetCohortSql = getCohortSql(entity.getOrigin(), analysis.getTreatmentId());
				result.add(new MockMultipartFile("file", getTargetCohortFilename(name), MediaType.TEXT_PLAIN_VALUE, targetCohortSql.getBytes()));

				String comparatorCohortSql = getCohortSql(entity.getOrigin(), analysis.getComparatorId());
				result.add(new MockMultipartFile("file", getComparatorCohortFilename(name), MediaType.TEXT_PLAIN_VALUE, comparatorCohortSql.getBytes()));

				String outcomeCohortSql = getCohortSql(entity.getOrigin(), analysis.getOutcomeId());
				result.add(new MockMultipartFile("file", getOutcomeCohortFilename(name), MediaType.TEXT_PLAIN_VALUE, outcomeCohortSql.getBytes()));

				String runnerR = buildRunner(name);
				result.add(new MockMultipartFile("file", "main.r", MediaType.TEXT_PLAIN_VALUE, runnerR.getBytes()));

			} catch (IOException | NoSuchMethodException | ScriptException e) {
				LOGGER.error("Failed to construct estimation", e);
			}
		}
		return result;
	}

	private String buildEstimationDesign(ComparativeCohortAnalysisInfo info)
					throws IOException, ScriptException, NoSuchMethodException {

		ObjectMapper mapper = new ObjectMapper();
		String infoJson = mapper.writeValueAsString(info);

		Resource jsResource = new ClassPathResource("estimation/EstimationBuilder.js");
		InputStream jsResourceStream = jsResource.getInputStream();
		String jsCode = IOUtils.toString(jsResourceStream, "UTF-8");
		jsResourceStream.close();

		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine.eval(jsCode);

		Invocable invocable = (Invocable) engine;
		Object result = invocable.invokeFunction("build", infoJson);

		return (String) result;
	}

	private String buildRunner(String analysisName) throws IOException {

		Map<String, Object> parameters = getRunnerParams(analysisName);

		return runnerTemplate.apply(parameters);
	}

	protected Map<String, Object> getRunnerParams(String analysisName) {

		Map<String, Object> parameters = new HashMap<>();

		parameters.put("analysisFile", getEstimationFilename(analysisName));
		parameters.put("targetCohort", getTargetCohortFilename(analysisName));
		parameters.put("comparatorCohort", getComparatorCohortFilename(analysisName));
		parameters.put("outcomeCohort", getOutcomeCohortFilename(analysisName));

		return parameters;
	}

	private String getEstimationFilename(String analysisName) {

		return analysisName + CommonFileUtils.ESTIMATION_EXT;
	}

	private String getTargetCohortFilename(String analysisName) {

		return getCohortFilename(combineName(analysisName, "target"));
	}

	private String getComparatorCohortFilename(String analysisName) {

		return getCohortFilename(combineName(analysisName, "comparator"));
	}

	private String getOutcomeCohortFilename(String analysisName) {

		return getCohortFilename(combineName(analysisName, "outcome"));
	}

	private String getCohortFilename(String cohortName) {

		return cohortName + CommonFileUtils.OHDSI_SQL_EXT;
	}

	private String combineName(String prefix, String name) {

		return prefix + "_" + name;
	}

	private String getCohortSql(Atlas origin, Integer cohortId) throws IOException {

		CohortDefinition definition = atlasService.execute(origin, atlasClient -> atlasClient.getCohortDefinition(cohortId));
		ObjectMapper mapper = new ObjectMapper();
		CohortExpression expression = mapper.readValue(definition.getExpression(), CohortExpression.class);
		final CohortExpressionQueryBuilder.BuildExpressionQueryOptions options
						= new CohortExpressionQueryBuilder.BuildExpressionQueryOptions();
		return ignorePreprocessingMark(queryBuilder.buildExpressionQuery(expression, options));
	}
}
