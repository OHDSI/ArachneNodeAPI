package com.odysseusinc.arachne.datanode.service.messaging.prediction;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.datanode.dto.atlas.PredictionAnalysis;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AnalysisInfoBuilder;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.messaging.BaseAtlas2_7Mapper;
import com.odysseusinc.arachne.datanode.service.messaging.EntityMapper;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class PredictionAtlas2_7Mapper extends BaseAtlas2_7Mapper<PredictionAnalysis> implements EntityMapper<PredictionAnalysis, CommonEntity, AtlasClient2_7> {

	private static final String PACKAGE_TMPL = "PredictionStudy%d";
	private final Template predictionRunnerTemplate;

	public PredictionAtlas2_7Mapper(AtlasService atlasService, AnalysisInfoBuilder analysisInfoBuilder, Template predictionRunnerTemplate) {

		super(atlasService, analysisInfoBuilder);
		this.predictionRunnerTemplate = predictionRunnerTemplate;
	}

	@Override
	public List<MultipartFile> mapEntity(CommonEntity entity) {

		final Integer localId = entity.getLocalId();
		return this.<AtlasClient2_7>doMapping(entity, atlasClient -> atlasClient.getPrediction(localId));
	}

	@Override
	protected Template getRunnerTemplate() {

		return predictionRunnerTemplate;
	}

	@Override
	protected String getPackageName(CommonEntity entity) {

		return String.format(PACKAGE_TMPL, entity.getLocalId());
	}

	@Override
	public List<PredictionAnalysis> getEntityList(AtlasClient2_7 client) {

		return client.getPatientLevelPredictions();
	}
}
