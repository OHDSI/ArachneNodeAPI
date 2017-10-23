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
 * Created: October 31, 2016
 *
 */

package com.odysseusinc.arachne.datanode.repository;

import static com.odysseusinc.arachne.datanode.security.RolesConstants.ROLE_ADMIN;

import com.odysseusinc.arachne.datanode.model.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findOneByEmail(String email);

    Optional<User> findOneById(Long userId);

    List<User> findByRoles_name(String roleAdmin, Sort sort);

    @Query(nativeQuery = true, value = "select * from users u "
            + " where (lower(u.first_name) similar to :suggestRequest or\n"
            + "                            lower(u.last_name) similar to :suggestRequest) "
            + " and u.id not in\n"
            + "                            (select user_id from users_roles ur\n"
            + "                              LEFT JOIN roles r on ur.role_id=r.id\n"
            + "                              WHERE  r.name='" + ROLE_ADMIN + "'\n"
            + "                            )"
            + " limit :limit")
    List<User> suggestNotAdmin(@Param("suggestRequest") String suggestRequest, @Param("limit") Integer limit);
}
