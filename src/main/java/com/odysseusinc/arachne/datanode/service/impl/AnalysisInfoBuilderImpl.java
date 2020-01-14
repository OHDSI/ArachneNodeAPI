package com.odysseusinc.arachne.datanode.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.service.AnalysisInfoBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AnalysisInfoBuilderImpl implements AnalysisInfoBuilder {

    private static final Logger log = LoggerFactory.getLogger(AnalysisInfoBuilderImpl.class);

    @Override
    public String generateCountAnalysisDescription(CohortDefinition cohort) {

        StringBuilder description = new StringBuilder();
        if (StringUtils.isNotBlank(cohort.getDescription())) {
            appendLine(description, "Cohort: " + cohort.getName());
            appendLine(description, "Description: " + cohort.getDescription());
        }
        appendLine(description, "Created by: " + cohort.getCreatedBy());
        return description.toString();
    }

    @Override
    public String generateHeraclesAnalysisDescription(CohortDefinition cohort) {

        StringBuilder description = new StringBuilder();
        appendLine(description, "Heracles analysis of the Cohort: " + cohort.getName());
        appendLine(description, "Cohort Description: " + cohort.getDescription());
        return description.toString();    }

    @Override
    public String generateCCAnalysisDescription(JsonNode analysisJson) {

        StringBuilder description = new StringBuilder();
        try {
            appendLine(description, "included cohorts:");
            final JsonNode cohorts = analysisJson.get("cohorts");
            for (JsonNode cohortNode : cohorts) {
                appendLine(description, cohortNode.get("name").asText());
            }
            appendLine(description, StringUtils.EMPTY);

            appendLine(description, "List of the featured analyses:");
            final ArrayNode featuredAnalyses = (ArrayNode) analysisJson.get("featureAnalyses");
            for (JsonNode analysis : featuredAnalyses) {
                appendLine(description, analysis.get("name").asText());
            }
        } catch (Exception ex) {
            log.warn("Cannot build analysis description: {}", analysisJson);
        }
        return description.toString();
    }

    @Override
    public String generatePathwayAnalysisDescription(String analysisName, JsonNode targetCohortsNode, JsonNode eventCohortsNode) {

        StringBuilder description = new StringBuilder();
        appendLine(description, analysisName);
        appendLine(description, StringUtils.EMPTY);

        try {
            appendLine(description, "target cohorts:");
            for (JsonNode targetCohortNode : targetCohortsNode) {
                appendLine(description, targetCohortNode.get("name").asText());
            }
            appendLine(description, StringUtils.EMPTY);

            appendLine(description, "event cohorts:");
            for (JsonNode eventCohortNode : eventCohortsNode) {
                appendLine(description, eventCohortNode.get("name").asText());
            }
        } catch (Exception ex) {
            log.warn("Cannot build analysis description: {} {}", targetCohortsNode, eventCohortsNode);
        }
        return description.toString();
    }


    private static void appendLine(StringBuilder builder, String text) {

        builder.append(text);
        builder.append(System.lineSeparator());
    }
}
