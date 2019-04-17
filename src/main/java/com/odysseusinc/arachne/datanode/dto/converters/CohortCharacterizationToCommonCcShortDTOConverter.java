package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonCcShortDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortCharacterization;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class CohortCharacterizationToCommonCcShortDTOConverter extends BaseAtlasEntityToCommonEntityDTOConverter<CohortCharacterization, CommonCcShortDTO> {

	public CohortCharacterizationToCommonCcShortDTOConverter(GenericConversionService conversionService) {

		super(conversionService);
	}

	@Override
	public CommonCcShortDTO convert(CohortCharacterization source) {

		CommonCcShortDTO dto = super.convert(source);
		dto.setLocalId(source.getId());
		dto.setModified(source.getUpdatedAt());
		dto.setType(CommonAnalysisType.COHORT_CHARACTERIZATION);
		return dto;
	}

	@Override
	protected CommonCcShortDTO getTargetClass() {

		return new CommonCcShortDTO();
	}
}
