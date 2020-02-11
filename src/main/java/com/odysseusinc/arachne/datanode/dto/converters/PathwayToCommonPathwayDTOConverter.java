package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonPathwayDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.Pathway;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
public class PathwayToCommonPathwayDTOConverter extends BaseAtlasEntityToCommonEntityDTOConverter<Pathway, CommonPathwayDTO> {
    public PathwayToCommonPathwayDTOConverter(GenericConversionService conversionService) {

        super(conversionService);
    }

    @Override
    public CommonPathwayDTO convert(Pathway source) {

        CommonPathwayDTO dto = super.convert(source);
        dto.setLocalId(source.getId());
        dto.setModified(source.getModifiedDate());
        dto.setType(CommonAnalysisType.COHORT_PATHWAY);
        return dto;
    }

    @Override
    protected CommonPathwayDTO getTargetClass() {

        return new CommonPathwayDTO();
    }
}
