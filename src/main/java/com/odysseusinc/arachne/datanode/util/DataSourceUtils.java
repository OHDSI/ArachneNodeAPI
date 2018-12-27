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
 * Created: September 19, 2017
 *
 */

package com.odysseusinc.arachne.datanode.util;

import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.dto.atlas.AtlasDetailedDTO;
import com.odysseusinc.arachne.datanode.dto.datasource.DataSourceDTO;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.util.datasource.QueryProcessor;
import com.odysseusinc.arachne.datanode.util.datasource.ResultSetContainer;
import com.odysseusinc.arachne.datanode.util.datasource.ResultSetProcessor;
import com.odysseusinc.arachne.datanode.util.datasource.ResultTransformer;
import com.odysseusinc.arachne.datanode.util.datasource.ResultWriter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

public class DataSourceUtils<T> {

    private final DataSource dataSource;
    private Connection c;
    private ResultSet resultSet;
    private Map results;
    private T transformed;

    public DataSourceUtils(DataSource dataSource) {

        this.dataSource = dataSource;
    }

    public static boolean isNotDummyPassword(String dbPassword) {

        return Objects.nonNull(dbPassword) && !Objects.equals(dbPassword, Constants.DUMMY_PASSWORD);
    }

    public static <T> DataSourceUtils<T> withDataSource(DataSource dataSource) {

        return new DataSourceUtils<>(dataSource);
    }

    public static void masqueradePassword(DataSourceDTO dataSource) {

        dataSource.setDbPassword(getMasqueradedPassword(dataSource.getDbPassword()));
        dataSource.setKrbPassword(getMasqueradedPassword(dataSource.getKrbPassword()));
    }

    public static void masqueradePassword(AtlasDetailedDTO atlasDetailedDTO) {

        atlasDetailedDTO.setPassword(getMasqueradedPassword(atlasDetailedDTO.getPassword()));
    }

    private static String getMasqueradedPassword(String password) {

        return StringUtils.isEmpty(password) ? "" : Constants.DUMMY_PASSWORD;
    }

    public DataSourceUtils<T> ifTableNotExists(String schema, String tableName, Function<String, RuntimeException> handler) throws SQLException {

        Objects.requireNonNull(handler, "Handler function is required");
        createConnection();
        DatabaseMetaData metaData = c.getMetaData();
        ResultSet resultSet = metaData.getTables(null, schema, tableName, null);
        if (!resultSet.next()) {
            throw handler.apply(tableName);
        }
        return this;
    }

    public DataSourceUtils<T> run(QueryProcessor queryProcessor) throws SQLException {

        Objects.requireNonNull(queryProcessor, "queryProcessor is required");
        createConnection();
        resultSet = queryProcessor.process(c);
        return this;
    }

    private void createConnection() throws SQLException {

        if (c == null || c.isClosed()) {
            String user = dataSource.getUsername();
            String password = dataSource.getPassword();
            String url = dataSource.getConnectionString();
            c = DriverManager.getConnection(url, user, password);
            c.setAutoCommit(false);
        }
    }

    public DataSourceUtils<T> collectResults(ResultSetProcessor<Map> processor) throws SQLException {

        Objects.requireNonNull(processor, "resultSetProcessor is required");
        Objects.requireNonNull(c, "Connection was not established");
        Objects.requireNonNull(resultSet, "try to run query first");
        try (ResultSet rs = this.resultSet) {
            results = processor.process(rs).getValues();
        } finally {
            if (c != null) {
                c.close();
            }
            return this;
        }
    }

    public DataSourceUtils<T> mapResults(String key, ResultSetProcessor<Map> processor) throws SQLException {

        Objects.requireNonNull(processor, "resultSetProcessor is required");
        Objects.requireNonNull(c, "Connection was not established");
        Objects.requireNonNull(resultSet, "Try to run query first");
        if (Objects.isNull(this.results)) {
            this.results = new HashMap();
        }
        try (ResultSet rs = this.resultSet) {
            Map proceed = processor.process(rs).getValues();
            this.results.merge(key, proceed, (old, value) -> {
                ((Map) old).putAll((Map) value);
                return old;
            });
        } finally {
            c.close();
        }
        return this;
    }

    public DataSourceUtils<T> forMapResults(List identities, String identityKey, String key,
                                            ResultSetProcessor<Map> processor) throws SQLException {

        Objects.requireNonNull(processor, "resultSetProcessor is required");
        Objects.requireNonNull(c, "Connection was not established");
        Objects.requireNonNull(resultSet, "Try to run query first");
        if (Objects.isNull(this.results)) {
            this.results = new HashMap();
        }
        try (ResultSet rs = this.resultSet) {
            final ResultSetContainer<Map> resultContainer = processor.process(rs);
            Map proceed = resultContainer.getValues();
            identities.forEach(id -> {
                List<Map> values = (List<Map>) proceed.getOrDefault(id, resultContainer.getDefaultValue());
                Map filtered = new HashMap();
                values.forEach(map -> {
                    map.keySet().forEach(col -> {
                        if (!Objects.equals(identityKey, col)) {
                            filtered.merge(col, new LinkedList(Arrays.asList(map.get(col))), (list, val) -> {
                                ((List) list).addAll((Collection) val);
                                return list;
                            });
                        }
                    });
                });
                if (!filtered.isEmpty()) {
                    this.results.putIfAbsent(id, new HashMap<>());
                    Map<String, Map> data = (Map<String, Map>) this.results.get(id);
                    data.merge(key, filtered, (old, val) -> {
                        old.keySet().forEach(columnName -> {
                            ((List) old.get(columnName)).addAll(((List) val.getOrDefault(columnName, new ArrayList<>())));
                        });
                        return old;
                    });
                }
            });
        } finally {
            c.close();
        }
        return this;
    }

    public DataSourceUtils<T> transform(ResultTransformer<Map, T> transformer) {

        Objects.requireNonNull(transformer);
        Objects.requireNonNull(results, "results was not collected");
        transformed = transformer.transform(results);
        return this;
    }

    public DataSourceUtils<T> transmitResults(Consumer<Map> consumer) {

        if (Objects.nonNull(consumer)) {
            consumer.accept(results);
        }
        return this;
    }

    public DataSourceUtils<T> write(ResultWriter<T> writer) throws IOException {

        Objects.requireNonNull(writer);
        Objects.requireNonNull(transformed);
        writer.write(transformed);
        return this;
    }

    public void forEach(List<Integer> identifiers) {

    }

    public Map getResults() {

        return results;
    }

    public Integer getResultsCount() {

        return Objects.nonNull(results) ? results.size() : 0;
    }
}
