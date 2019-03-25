package com.odysseusinc.arachne.datanode.service.messaging.prediction;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.messaging.BaseAtlas2_7Mapper;
import com.odysseusinc.arachne.datanode.service.messaging.EntityMapper;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class PredictionAtlas2_7Mapper extends BaseAtlas2_7Mapper implements EntityMapper<CommonEntity> {

	private static final String PACKAGE_TMPL = "PredictionStudy%d";
	private final Template predictionRunnerTemplate;

	public PredictionAtlas2_7Mapper(AtlasService atlasService, Template predictionRunnerTemplate) {

		super(atlasService);
		this.predictionRunnerTemplate = predictionRunnerTemplate;
	}

	@Override
	public List<MultipartFile> mapEntity(CommonEntity entity) {

		final Integer localId = entity.getLocalId();
		final String packageName = getPackageName(entity);
		return this.<AtlasClient2_7>doMapping(entity, atlasClient -> atlasClient.getPrediction(localId, packageName));
	}

	@Override
	protected Template getRunnerTemplate() {

		return predictionRunnerTemplate;
	}

	@Override
	protected String getPackageName(CommonEntity entity) {

		return String.format(PACKAGE_TMPL, entity.getLocalId());
	}
}
