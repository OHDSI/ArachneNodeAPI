/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Jul 8, 2019
 *
 */

package com.odysseusinc.arachne.datanode.repository;

import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findById(Long id);

    @Query(nativeQuery = true, value =
            "SELECT analyses.* "
                    + "FROM analyses "
                    + " JOIN analysis_state_journal AS journal ON journal.analysis_id = analyses.id "
                    + " JOIN (SELECT analysis_id, max(date) AS latest FROM analysis_state_journal "
                    + " GROUP BY analysis_id) AS FOO ON journal.date = FOO.latest AND journal.analysis_id=FOO.analysis_id "
                    + " WHERE analyses.id = :id AND analyses.callback_password = :password AND journal.state = 'EXECUTING'")
    Optional<Analysis> findOneExecuting(@Param("id") Long id, @Param("password") String password);

    @Query(nativeQuery = true, value =
            "SELECT analyses.* "
            + "FROM analyses "
            + " JOIN analysis_state_journal AS journal ON journal.analysis_id = analyses.id "
            + " JOIN (SELECT analysis_id, max(date) AS latest FROM analysis_state_journal "
            + " GROUP BY analysis_id) AS FOO ON journal.date = FOO.latest AND journal.analysis_id=FOO.analysis_id "
            + " WHERE journal.state = :state AND analyses.central_id IS NOT NULL")
    List<Analysis> findAllByState(@Param("state") String state);

    @Query(nativeQuery = true, value =
            "SELECT analyses.* "
                    + "FROM analyses "
                    + " JOIN analysis_state_journal AS journal ON journal.analysis_id = analyses.id "
                    + " JOIN (SELECT analysis_id, max(date) AS latest FROM analysis_state_journal "
                    + " GROUP BY analysis_id) AS FOO ON journal.date = FOO.latest AND journal.analysis_id=FOO.analysis_id "
                    + " WHERE journal.state NOT IN (:states)")
    List<Analysis> findAllByNotStateIn(@Param("states") List<String> states);

    @Query(nativeQuery = true, value =
            "SELECT analyses.* "
            + " FROM analyses "
            + " JOIN analysis_state_journal AS journal ON journal.analysis_id = analyses.id "
            + " JOIN (SELECT analysis_id, max(date) AS latest FROM analysis_state_journal "
            + " GROUP BY analysis_id) AS FOO ON journal.date = FOO.latest AND journal.analysis_id=FOO.analysis_id "
            + " WHERE journal.state = :state AND journal.date < :time")
    List<Analysis> findAllExecutingMoreThan(@Param("state") String state, @Param("time") Date time);

    @Query(nativeQuery = true, value =
            "select a.* from analyses a "
                    + " JOIN analysis_state_journal AS journal ON journal.analysis_id = a.id "
                    + " JOIN (SELECT analysis_id, max(date) AS latest FROM analysis_state_journal "
                    + " GROUP BY analysis_id) AS FOO ON journal.date = FOO.latest AND journal.analysis_id=FOO.analysis_id"
                    + " \n--#pageable\n",
        countQuery = "select count(a.*) from analyses a "
                + " JOIN analysis_state_journal AS journal ON journal.analysis_id = a.id "
                + " JOIN (SELECT analysis_id, max(date) AS latest FROM analysis_state_journal "
                + " GROUP BY analysis_id) AS FOO ON journal.date = FOO.latest AND journal.analysis_id=FOO.analysis_id")
    Page<Analysis> findAllPagedOrderByState(Pageable pageable);

    @Query(nativeQuery = true, value =
            "select a.* from analyses a "
                    + " JOIN analysis_state_journal AS journal ON journal.analysis_id = a.id "
                    + " JOIN (SELECT analysis_id, min(date) AS submitted FROM analysis_state_journal "
                    + " GROUP BY analysis_id) AS SUB ON journal.date = SUB.submitted AND journal.analysis_id=SUB.analysis_id"
                    + " \n--#pageable\n",
            countQuery = "select count(a.*) from analyses a "
                    + " JOIN analysis_state_journal AS journal ON journal.analysis_id = a.id "
                    + " JOIN (SELECT analysis_id, min(date) AS submitted FROM analysis_state_journal "
                    + " GROUP BY analysis_id) AS SUB ON journal.date = SUB.submitted AND journal.analysis_id=SUB.analysis_id")
    Page<Analysis> findAllPagedOrderBySubmitted(Pageable pageable);

    @Query(nativeQuery = true, value =
            "select a.*, "
                    + "case journal.state "
                    + "  when 'EXECUTING' then null "
                    + "  when 'EXECUTION_READY' then null "
                    + "  when 'CREATED' then null"
                    + "  else journal.date "
                    + "end as finished FROM analyses a "
                    + " JOIN analysis_state_journal AS journal ON journal.analysis_id = a.id "
                    + " JOIN (SELECT analysis_id, max(date) AS finished FROM analysis_state_journal "
                    + " GROUP BY analysis_id) AS SUB ON journal.date = SUB.finished AND journal.analysis_id=SUB.analysis_id"
                    + " \n--#pageable\n",
            countQuery = "select count(a.*) from analyses a "
                    + " JOIN analysis_state_journal AS journal ON journal.analysis_id = a.id "
                    + " JOIN (SELECT analysis_id, max(date) AS finished FROM analysis_state_journal "
                    + " GROUP BY analysis_id) AS SUB ON journal.date = SUB.finished AND journal.analysis_id=SUB.analysis_id")
    Page<Analysis> findAllPagedOrderByFinished(Pageable pageable);
}
