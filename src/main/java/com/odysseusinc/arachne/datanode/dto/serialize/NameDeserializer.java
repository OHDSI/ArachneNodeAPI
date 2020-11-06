/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Alexandr Cumarav, Vitaly Koulakov,
 * Created: October 29, 2020
 *
 */

package com.odysseusinc.arachne.datanode.dto.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;

public class NameDeserializer extends JsonDeserializer<String> {

    private static final String NAME_FIELD = "name";

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        if (Objects.isNull(jsonParser) || StringUtils.isBlank(jsonParser.getText())) {
            return null;
        }

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.isValueNode()) {
            return node.asText();
        }
        if (node.has(NAME_FIELD)) {
            return node.get(NAME_FIELD).asText();
        }
        return null;
    }
}