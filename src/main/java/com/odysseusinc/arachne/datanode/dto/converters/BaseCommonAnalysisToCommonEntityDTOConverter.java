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

package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.CommonAnalysis;
import org.springframework.core.convert.support.GenericConversionService;

public abstract class BaseCommonAnalysisToCommonEntityDTOConverter<S extends CommonAnalysis, D extends CommonEntityDTO> extends BaseAtlasEntityToCommonEntityDTOConverter<S, D> {

	public BaseCommonAnalysisToCommonEntityDTOConverter(GenericConversionService conversionService) {

		super(conversionService);
	}

	@Override
	public D convert(S source) {

		D dto = super.convert(source);
		dto.setLocalId(source.getId().longValue());
		dto.setName(source.getName());
		dto.setDescription(source.getDescription());
		dto.setModified(source.getModifiedDate());
		return dto;
	}

}
