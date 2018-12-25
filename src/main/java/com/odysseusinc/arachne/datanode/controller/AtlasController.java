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
 * Authors: Pavel Grafkin
 * Created: March 15, 2018
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import com.odysseusinc.arachne.commons.api.v1.dto.AtlasShortDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.AtlasDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.AtlasDetailedDTO;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.AtlasService;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AtlasController {

    private final AtlasService atlasService;
    private final ConversionService conversionService;

    public AtlasController(AtlasService atlasService, ConversionService conversionService) {

        this.atlasService = atlasService;
        this.conversionService = conversionService;
    }

    @ApiOperation("List all Atlases")
    @RequestMapping(value = "/api/v1/atlases", method = GET)
    public Page<AtlasDTO> list(
            @PageableDefault(page = 1)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "name", direction = Sort.Direction.ASC)
            }) Pageable pageable
    ) {

        Pageable search = new PageRequest(pageable.getPageNumber() - 1, pageable.getPageSize(), pageable.getSort());
        Page<Atlas> atlasPage = atlasService.findAll(search);
        return atlasPage.map(a -> conversionService.convert(a, AtlasDTO.class));
    }

    @ApiOperation("Get Atlas detailed info")
    @RequestMapping(value = "/api/v1/atlases/{id}", method = GET)
    public AtlasDetailedDTO get(@PathVariable("id") Long id) {

        Atlas atlas = atlasService.getById(id);
        return conversionService.convert(atlas, AtlasDetailedDTO.class);
    }

    @ApiOperation("Create new Atlas")
    @RequestMapping(value = "/api/v1/atlases", method = POST)
    public AtlasDetailedDTO save(@RequestBody AtlasDetailedDTO atlasDetailedDTO) {

        Atlas atlas = conversionService.convert(atlasDetailedDTO, Atlas.class);
        atlas = atlasService.save(atlas);
        return conversionService.convert(atlas, AtlasDetailedDTO.class);
    }

    @ApiOperation("Update Atlas entity")
    @RequestMapping(value = "/api/v1/atlases/{id}", method = PUT)
    public AtlasDetailedDTO update(
            @PathVariable("id") Long id,
            @RequestBody AtlasDetailedDTO atlasDetailedDTO
    ) {

        Atlas atlas = conversionService.convert(atlasDetailedDTO, Atlas.class);
        atlas = atlasService.update(id, atlas);
        return conversionService.convert(atlas, AtlasDetailedDTO.class);
    }

    @ApiOperation("Delete Atlas entity")
    @RequestMapping(value = "/api/v1/atlases/{id}", method = DELETE)
    public void delete(
            @PathVariable("id") Long id
    ) {

        atlasService.delete(id);
    }
}
