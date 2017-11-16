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
 * Created: June 13, 2017
 *
 */

package com.odysseusinc.arachne.datanode.controller;

import java.util.Objects;

public class API {
    static String STATUS_METHOD = "/api/v1/achilles/%d/status";
    static String HISTORY_METHOD = "/api/v1/achilles/%d/jobs";
    static String LOG_METHOD = "/api/v1/achilles/%d/log/%d";

    static String achillesJobStatus(Long datasourceId) {

        Objects.requireNonNull(datasourceId);
        return String.format(STATUS_METHOD, datasourceId);
    }

    static String achillesJobHistory(Long datasourceId) {

        Objects.nonNull(datasourceId);
        return String.format(HISTORY_METHOD, datasourceId);
    }

    public static String achillesJobLog(Long datasourceId, long timestamp) {

        Objects.nonNull(datasourceId);
        return String.format(LOG_METHOD, datasourceId, timestamp);
    }
}
