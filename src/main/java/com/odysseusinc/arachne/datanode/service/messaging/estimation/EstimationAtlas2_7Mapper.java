package com.odysseusinc.arachne.datanode.service.messaging.estimation;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.datanode.dto.atlas.EstimationAnalysis;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AnalysisInfoBuilder;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.messaging.BaseAtlas2_7Mapper;
import com.odysseusinc.arachne.datanode.service.messaging.EntityMapper;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class EstimationAtlas2_7Mapper extends BaseAtlas2_7Mapper<EstimationAnalysis> implements EntityMapper<EstimationAnalysis, CommonEntity, AtlasClient2_7> {

	private static final String PACKAGE_TMPL = "EstimationStudy%s";
	private Template estimationRunnerTemplate;

	public EstimationAtlas2_7Mapper(AtlasService atlasService, Template estimationRunnerTemplate, AnalysisInfoBuilder analysisInfoBuilder) {

		super(atlasService, analysisInfoBuilder);
		this.estimationRunnerTemplate = estimationRunnerTemplate;
	}

	@Override
	protected String getPackageName(CommonEntity entity) {

		return String.format(PACKAGE_TMPL, entity.getLocalId());
	}

	@Override
	protected Template getRunnerTemplate() {

		return estimationRunnerTemplate;
	}

	@Override
	public List<EstimationAnalysis> getEntityList(AtlasClient2_7 client) {

		return client.getEstimations();
	}

	@Override
	public List<MultipartFile> mapEntity(CommonEntity entity) {

		final Integer localId = entity.getLocalId();
		return this.<AtlasClient2_7>doMapping(entity, atlasClient -> atlasClient.getEstimation(localId));
	}
}
