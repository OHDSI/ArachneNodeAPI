package com.odysseusinc.arachne.datanode.service.messaging;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonPathwayDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.Pathway;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.AtlasRequestHandler;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import com.odysseusinc.arachne.datanode.service.CommonEntityService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PathwayRequestHandler extends BaseRequestHandler implements AtlasRequestHandler<CommonPathwayDTO, List<MultipartFile>> {

    private final GenericConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private static final int PAGE_SIZE = 10000;
    public static final String PATHWAY_BUILD_ERROR = "Failed to build Pathway data";

    @Autowired
    public PathwayRequestHandler(SqlRenderService sqlRenderService,
                                 AtlasService atlasService,
                                 CommonEntityService commonEntityService,
                                 GenericConversionService conversionService) {

        super(sqlRenderService, atlasService);
        this.commonEntityService = commonEntityService;
        this.conversionService = conversionService;
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
            Map<String, Object> pathway = atlasService.execute(entity.getOrigin(), atlasClient -> atlasClient.getPathway(entity.getLocalId()));
            List<MultipartFile> files = new ArrayList<>();


            return files;
        }).orElse(null);
    }

    @Override
    public CommonAnalysisType getAnalysisType() {

        return CommonAnalysisType.COHORT_PATHWAY;
    }

    @Override
    public void sendResponse(List<MultipartFile> response, String id) {

    }
}
