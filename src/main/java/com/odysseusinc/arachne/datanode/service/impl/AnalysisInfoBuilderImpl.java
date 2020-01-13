package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.service.AnalysisInfoBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class AnalysisInfoBuilderImpl implements AnalysisInfoBuilder {

    @Override
    public String generateAnalysisDescription(CohortDefinition definition) {

        StringBuilder description = new StringBuilder();
        if (StringUtils.isNotBlank(definition.getDescription())) {
            appendLine(description, "Cohort: " + definition.getName());
            appendLine(description, "Description: " + definition.getDescription());
        }
        appendLine(description, "Created by: " + definition.getCreatedBy());
        return description.toString();
    }

    private static void appendLine(StringBuilder builder, String text){
        builder.append(text);
        builder.append(System.lineSeparator());
    }
}
