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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: December 05, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.odysseusinc.arachne.commons.api.v1.dto.AtlasShortDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.dto.converters.AtlasShortDTOToAtlasConverter;
import com.odysseusinc.arachne.datanode.dto.converters.AtlasToAtlasShortDTOConverter;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasInfoClient;
import com.odysseusinc.arachne.datanode.service.postpone.annotation.Postponed;
import com.odysseusinc.arachne.datanode.service.postpone.annotation.PostponedArgument;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AtlasService {

    String checkConnection(Atlas atlas);

    List<Atlas> findAll();

    Page<Atlas> findAll(Pageable pageable);

    Atlas getById(Long id);

    Atlas updateVersion(Long atlasId, String version);

    Atlas save(Atlas atlas);

    Atlas update(Long atlasId, Atlas atlas);

    void delete(Long atlasId);

//    @Postponed(action = "delete")
    void deleteFromCentral(@PostponedArgument(serializer = AtlasToAtlasShortDTOConverter.class, deserializer = AtlasShortDTOToAtlasConverter.class) Atlas atlas);

    <C extends AtlasClient, R extends BaseAtlasEntity> List<R> execute(List<Atlas> atlasList, Function<C, ? extends List<R>> sendAtlasRequest);

    <C extends AtlasClient, R> R execute(Atlas atlas, Function<C, R> sendAtlasRequest);

    <R> R executeInfo(Atlas atlas, Function<AtlasInfoClient, R> sendAtlasRequest);

    byte[] hydrateAnalysis(JsonNode analysis, String packageName) throws IOException;

    byte[] hydrateAnalysis(JsonNode analysis, String packageName, String skeletonResource) throws IOException;

//    @Postponed(action = "update")
    AtlasShortDTO updateOnCentral(Atlas atlas);
}
