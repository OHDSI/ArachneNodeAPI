package com.odysseusinc.arachne.datanode.service.messaging.estimation;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.messaging.BaseAtlas2_7Mapper;
import com.odysseusinc.arachne.datanode.service.messaging.EntityMapper;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class EstimationAtlas2_7Mapper extends BaseAtlas2_7Mapper implements EntityMapper<CommonEntity> {

	private static final String PACKAGE_TMPL = "EstimationStudy%s";
	private Template estimationRunnerTemplate;

	public EstimationAtlas2_7Mapper(AtlasService atlasService, Template estimationRunnerTemplate) {

		super(atlasService);
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
	public List<MultipartFile> mapEntity(CommonEntity entity) {

		final Integer localId = entity.getLocalId();
		final String packageName = getPackageName(entity);
		return this.<AtlasClient2_7>doMapping(entity, atlasClient -> atlasClient.getEstimation(localId, packageName));
	}
}
