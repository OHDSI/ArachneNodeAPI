/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Apr 26, 2019
 *
 */

package com.odysseusinc.arachne.datanode.service.messaging;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.model.atlas.CommonEntity;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.messaging.prediction.PredictionAtlas2_5Mapper;
import com.odysseusinc.arachne.datanode.service.messaging.prediction.PredictionAtlas2_7Mapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

public abstract class CommonAnalysisRequestHandler extends BaseRequestHandler {

	protected final Template legacyRunnerTemplate;
	protected final Template runnerTemplate;
	protected final CentralSystemClient centralClient;

	public CommonAnalysisRequestHandler(SqlRenderService sqlRenderService,
																			AtlasService atlasService,
																			Template runnerTemplate,
																			Template legacyRunnerTemplate,
																			CentralSystemClient centralClient) {

		super(sqlRenderService, atlasService);
		this.runnerTemplate = runnerTemplate;
		this.legacyRunnerTemplate = legacyRunnerTemplate;
		this.centralClient = centralClient;
	}

	protected List<BaseAtlasEntity> getEntities(List<Atlas> atlasList) {

		return atlasList.stream()
						.flatMap(atlas -> atlasService.execute(atlas, client ->  getEntityMapper(atlas)
										.getEntityList(client).stream().peek(en -> en.setOrigin(atlas))))
						.collect(Collectors.toList());
	}

	protected abstract  <T extends BaseAtlasEntity, C extends AtlasClient> EntityMapper<T, CommonEntity, C> getEntityMapper(Atlas atlas);

	public void sendResponse(List<MultipartFile> response, String id) {

			centralClient.sendCommonEntityResponse(id, response.toArray(new MultipartFile[0]));
	}
}
