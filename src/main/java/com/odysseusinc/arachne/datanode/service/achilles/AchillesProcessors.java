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
 * Created: September 19, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.achilles;

import com.odysseusinc.arachne.datanode.util.datasource.ResultSetContainer;
import com.odysseusinc.arachne.datanode.util.datasource.ResultSetProcessor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AchillesProcessors {

    private static final Logger LOGGER = LoggerFactory.getLogger(AchillesProcessors.class);

    public static ResultSetProcessor<Map> achillesHeel() {

        return resultSet -> {
            Map<String, Object> data = new HashMap<>();
            data.put("MESSAGES", resultSet().process(resultSet));
            return new ResultSetContainer<>(data, null);
        };
    }

    public static ResultSetProcessor<Map> resultSet(String... includeColumns) {

        List<String> includes = Arrays.asList(includeColumns);
        return resultSet -> {
            Map<String, List> data = new HashMap<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            Map<String, Integer> columns = getResultSetColumns(metaData, includes);
            while (resultSet.next()) {
                columns.keySet().forEach(column -> {
                    List<Object> value = new LinkedList<>();
                    try {
                        if (includes.isEmpty() || includes.contains(column)) {
                            value.add(getColumnValue(resultSet, column, columns.get(column)));
                        }
                    } catch (SQLException e) {
                        LOGGER.error("Failed to retrieve column data {}", column, e);
                    }
                    data.merge(column, value, (list, newValue) -> {
                        list.addAll(newValue);
                        return list;
                    });
                });
            }
            return new ResultSetContainer<>(data, null);
        };
    }

    public static ResultSetProcessor<Map> statsResultSet() {

        return resultSet -> {
            Map<String, Object> stats = new HashMap<>();
            if (resultSet.next()) {
                int min = resultSet.getInt("min_value");
                int max = resultSet.getInt("max_value");
                int size = resultSet.getInt("interval_size");
                stats.put("MIN", min);
                stats.put("MAX", max);
                stats.put("INTERVAL_SIZE", size);
                stats.put("INTERVALS", (max - min) / size);
            }
            return new ResultSetContainer<>(stats, null);
        };
    }

    public static ResultSetProcessor<Map> statsDataResultSet() {

        return resultSet -> {
            Map<String, Object> data = new HashMap<>();
            data.put("DATA", resultSet().process(resultSet));
            return new ResultSetContainer<>(data, null);
        };
    }

    public static ResultSetProcessor<Map> ageAtFirstResultSet() {

        return resultSet -> {
            Map<String, Object> hist = new HashMap<>();
            hist.put("MIN", 0);
            hist.put("MAX", 100);
            hist.put("INTERVAL_SIZE", 1);
            hist.put("INTERVALS", 100);
            Map data = resultSet().process(resultSet).getValues();
            hist.put("DATA", data);
            return new ResultSetContainer<>(hist, null);
        };
    }

    private static Map<String, Integer> getResultSetColumns(ResultSetMetaData metaData, List<String> includes) throws SQLException {

        includes = includes.stream().map(String::toUpperCase).collect(Collectors.toList());
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i).toUpperCase();
            if (includes.isEmpty() || includes.contains(columnName)) {
                columns.put(columnName, metaData.getColumnType(i));
            }
        }
        return columns;
    }

    public static <K> ResultSetProcessor<Map> plainResultSet(String primaryKey, String... includeColumns) {

        List<String> includes = new LinkedList<>(Arrays.asList(includeColumns));
        if (!includes.isEmpty()) {
            includes.add(primaryKey);
        }
        return resultSet -> {
            Map<K, List<Map>> data = new HashMap<>();
            Map<String, Integer> columns = getResultSetColumns(resultSet.getMetaData(), includes);
            while (resultSet.next()) {
                K key = (K) getColumnValue(resultSet, primaryKey, columns.getOrDefault(primaryKey.toUpperCase(), Types.VARCHAR));
                Map values = columns.entrySet().stream()
                        .filter(e -> !Objects.equals(e.getKey(), primaryKey))
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> {
                                    try {
                                        return getColumnValue(resultSet, e.getKey(), e.getValue());
                                    } catch (SQLException ignored) {
                                        return null;
                                    }
                                }
                        ));
                List<Map> list = new LinkedList<>();
                list.add(values);
                data.merge(key, list, (old, v) -> {
                    old.addAll(v);
                    return old;
                });
            }
            final Map<String, Object> defaultMap = Arrays.stream(includeColumns)
                    .collect(HashMap::new, (m, v) -> m.put(v.toUpperCase(), ""), HashMap::putAll);
            return new ResultSetContainer<>(data, Collections.singletonList(defaultMap));
        };
    }

    private static Object getColumnValue(ResultSet resultSet, String columnName, int type) throws SQLException {

        Object result;
        switch (type) {
            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                result = resultSet.getInt(columnName);
                break;
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGNVARCHAR:
                result = resultSet.getString(columnName);
                break;
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.REAL:
            case Types.DECIMAL:
                result = resultSet.getDouble(columnName);
                break;
            default:
                result = resultSet.getString(columnName);
                break;
        }
        return result;
    }
}
