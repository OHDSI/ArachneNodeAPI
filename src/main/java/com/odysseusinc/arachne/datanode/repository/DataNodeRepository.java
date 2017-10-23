/**
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
 * Created: November 01, 2016
 *
 */

package com.odysseusinc.arachne.datanode.repository;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonHealthStatus;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.stream.Stream;

public interface DataNodeRepository extends JpaRepository<DataNode, Long> {
    @Query("from DataNode dn where dn.sid = ?1")
    Optional<DataNode> findOneBySid(String sid);

    Optional<DataNode> findByCentralId(Long centralId);

    @Modifying
    @Query("UPDATE DataNode dn "
            + " SET dn.healthStatus = :healthStatus, dn.healthStatusDescription = :healthStatusDescription "
            + " WHERE dn.id = :id")
    void updateHealthStatus(@Param("id") Long id,
                            @Param("healthStatus") CommonHealthStatus healthStatus,
                            @Param("healthStatusDescription") String healthStatusDescription);

    Stream<DataNode> findAllByCentralIdIsNull();
}
