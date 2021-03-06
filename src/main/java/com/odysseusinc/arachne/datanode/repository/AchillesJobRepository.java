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
 * Created: June 08, 2017
 *
 */

package com.odysseusinc.arachne.datanode.repository;

import com.odysseusinc.arachne.datanode.model.achilles.AchillesJob;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;


public interface AchillesJobRepository extends CrudRepository<AchillesJob, Long> {

    @Transactional(readOnly = true)
    Optional<AchillesJob> findTopByDataSourceOrderByStarted(DataSource dataSource);

    @Transactional(readOnly = true)
    Optional<AchillesJob> findTopByDataSourceAndStatusOrderByStarted(DataSource dataSource, AchillesJobStatus status);

    @Transactional(readOnly = true)
    List<AchillesJob> findByStatus(AchillesJobStatus status);

    @Transactional(readOnly = true)
    List<AchillesJob> findByDataSourceOrderByStartedDesc(DataSource dataSource);

    @Transactional(readOnly = true)
    Page<AchillesJob> findByDataSource(DataSource dataSource, Pageable pageable);

    @Transactional(readOnly = true)
    Optional<AchillesJob> findByDataSourceAndStarted(DataSource dataSource, Timestamp started);
}
