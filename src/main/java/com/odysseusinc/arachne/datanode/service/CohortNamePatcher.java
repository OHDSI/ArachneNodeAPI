package com.odysseusinc.arachne.datanode.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface CohortNamePatcher {

    JsonNode patchAnalysis(JsonNode analysis);

}
