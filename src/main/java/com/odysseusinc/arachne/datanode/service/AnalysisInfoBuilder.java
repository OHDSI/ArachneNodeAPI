package com.odysseusinc.arachne.datanode.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;

public interface AnalysisInfoBuilder {

    String generateCountAnalysisDescription(CohortDefinition definition) ;

    String generateHeraclesAnalysisDescription(CohortDefinition cohortDefinition);

    String generateCCAnalysisDescription(JsonNode analysisJson);

    String generatePathwayAnalysisDescription(String analysisName, JsonNode targetCohortsNode, JsonNode eventCohortsNode);
}
