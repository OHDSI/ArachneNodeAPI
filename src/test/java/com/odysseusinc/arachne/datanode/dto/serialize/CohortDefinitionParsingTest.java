package com.odysseusinc.arachne.datanode.dto.serialize;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CohortDefinitionParsingTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp(){

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    @Test
    public void shouldParseCreatorNameFromStringValue() throws IOException {

        URL atlas28CohortDefinitionURL = CohortDefinitionParsingTest.class.getResource("/com/odysseusinc/arachne/datanode/service/Atlas27CohortDefinition.json");
        CohortDefinition cohortDefinition = objectMapper.readValue(atlas28CohortDefinitionURL, CohortDefinition.class);

        assertThat(cohortDefinition.getCreatedBy()).isEqualTo("odysseus.test2");
        assertThat(cohortDefinition.getModifiedBy()).isEqualTo("odysseus.test3");
    }

    @Test
    public void shouldParseCreatorNameFromUserDTOObject() throws IOException {

        URL atlas28CohortDefinitionURL = CohortDefinitionParsingTest.class.getResource("/com/odysseusinc/arachne/datanode/service/Atlas28CohortDefinition.json");
        CohortDefinition cohortDefinition = objectMapper.readValue(atlas28CohortDefinitionURL, CohortDefinition.class);

        assertThat(cohortDefinition.getCreatedBy()).isEqualTo("odysseus.test");
        assertThat(cohortDefinition.getModifiedBy()).isEqualTo("odysseus.test3");
    }
}