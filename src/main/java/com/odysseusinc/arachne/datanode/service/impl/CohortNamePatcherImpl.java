package com.odysseusinc.arachne.datanode.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.odysseusinc.arachne.datanode.service.CohortNamePatcher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class CohortNamePatcherImpl implements CohortNamePatcher {

    private static final String NAME_FIELD = "name";

    @Override
    public JsonNode patchAnalysis(JsonNode analysis) {

        JsonNode analysisClone = analysis.deepCopy();
        final JsonNode cohortsNode = analysisClone.get("cohorts");
        if (cohortsNode instanceof ArrayNode) {
            ArrayNode cohorts = (ArrayNode) cohortsNode;
            cohorts.elements().forEachRemaining(this::patchCohortName);
        }

        return analysisClone;
    }

    private void patchCohortName(JsonNode jsonNode) {

        if (jsonNode.has(NAME_FIELD)) {
            ObjectNode cohortNode = (ObjectNode) jsonNode;
            final String cohortName = jsonNode.get((NAME_FIELD)).asText();
            final String patchedName = StringUtils.replace(cohortName, "/", "");
            TextNode cohortNameNode = new TextNode(patchedName);
            cohortNode.replace(NAME_FIELD, cohortNameNode);
        }
    }
}
