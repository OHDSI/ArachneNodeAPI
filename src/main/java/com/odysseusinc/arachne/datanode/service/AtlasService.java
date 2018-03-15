/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
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

import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
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

    <R extends BaseAtlasEntity> List<R> execute(List<Atlas> atlasList, Function<? super AtlasClient, ? extends List<R>> sendAtlasRequest);

    <R> R execute(Atlas atlas, Function<? super AtlasClient, R> sendAtlasRequest);
}
