/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
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
 * Created: September 19, 2017
 *
 */

package com.odysseusinc.arachne.datanode.util.datasource;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultTransformers {

    public static <T> ResultTransformer<T, String> toJson() {

        return result -> new Gson().toJson(result);
    }

    public static <T extends Map> ResultTransformer<T, Map<Integer, String>> toJsonMap(List<Integer> identities) {

        return result -> {
            Gson gson = new Gson();
            Map<Integer, String> jsonMap = new HashMap<>();
            identities.forEach(id -> {
                if (result.containsKey(id)) {
                    String json = gson.toJson(result.get(id));
                    jsonMap.put(id, json);
                }
            });
            return jsonMap;
        };
    }
}
