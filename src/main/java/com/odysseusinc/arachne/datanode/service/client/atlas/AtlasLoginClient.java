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
 * Authors: Pavel Grafkin, Alexander Saltykov, Vitaly Koulakov, Anton Gackovka, Alexandr Ryabokon, Mikhail Mironov
 * Created: Nov 7, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.atlas;

import com.odysseusinc.arachne.datanode.Constants;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AtlasLoginClient {

    @RequestLine("POST " + Constants.Atlas.LOGIN_DB)
    @Headers("Content-Type: application/x-www-form-urlencoded")
    String loginDatabase(@Param("login") String username, @Param("password") String password);

    @RequestLine("POST " + Constants.Atlas.LOGIN_LDAP)
    @Headers("Content-Type: application/x-www-form-urlencoded")
    String loginLdap(@Param("login") String username, @Param("password") String password);
}
