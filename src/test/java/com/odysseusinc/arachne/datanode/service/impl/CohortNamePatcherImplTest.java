package com.odysseusinc.arachne.datanode.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.odysseusinc.arachne.datanode.service.CohortNamePatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CohortNamePatcherImplTest {

    private CohortNamePatcher cohortNamePatcher;

    @Before
    public void setUp() {

        cohortNamePatcher = new CohortNamePatcherImpl();
    }

    @Test
    public void shouldRemoveSlashesFromCohortNames() throws IOException {

        final InputStream studySpecIS = CohortNamePatcherImplTest.class.getResourceAsStream("/com/odysseusinc/arachne/datanode/service/StudyWithIncorrectCohortName.json");
        final JsonNode analysis = new ObjectMapper().readTree(studySpecIS);
        final JsonNode updatedAnalysis = cohortNamePatcher.patchAnalysis(analysis);
        final List<JsonNode> cohorts = parseCohortNodes(updatedAnalysis);
        final JsonNode cohort = cohorts.get(0);
        assertThat(cohort.get("name").asText()).isEqualTo("[COVID ID1 v1] Persons hospitalized with COVID-19 narrow w prior observation");
    }

    @Test
    public void shouldLeaveNameUntouched() throws IOException {

        final InputStream studySpecIS = CohortNamePatcherImplTest.class.getResourceAsStream("/com/odysseusinc/arachne/datanode/service/StudyWithIncorrectCohortName.json");
        final JsonNode analysis = new ObjectMapper().readTree(studySpecIS);
        final JsonNode updatedAnalysis = cohortNamePatcher.patchAnalysis(analysis);
        final List<JsonNode> cohorts = parseCohortNodes(updatedAnalysis);
        final JsonNode cohort = cohorts.get(1);
        assertThat(cohort.get("name").asText()).isEqualTo("simple name");
    }

    @Test
    public void shouldNotFailOnBadJSON() throws IOException {

        final InputStream studySpecIS = CohortNamePatcherImplTest.class.getResourceAsStream("/com/odysseusinc/arachne/datanode/service/StudyWithIncorrectJSON.json");
        final JsonNode analysis = new ObjectMapper().readTree(studySpecIS);
        final JsonNode updatedAnalysis = cohortNamePatcher.patchAnalysis(analysis);
        assertThat(updatedAnalysis).isNotNull();
    }

    private List<JsonNode> parseCohortNodes(JsonNode analysis) {

        final ArrayNode cohorts = (ArrayNode) analysis.get("cohorts");
        return Lists.newArrayList(cohorts.elements());
    }

}