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
 * Created: September 19, 2017
 *
 */

package com.odysseusinc.arachne.datanode.util.datasource;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;

public class ResultWriters {

    public static ResultWriter<String> toFile(String fileName) {

        return result -> {
            try (Writer writer = new FileWriter(fileName)) {
                IOUtils.write(result, writer);
            }
        };
    }

    public static ResultWriter<String> toFile(Path path) {

        return toFile(path.toString());
    }

    public static ResultWriter<Map<Integer, String>> toMultipleFiles(Path targetDir, String filenamePattern,
                                                                     List<Integer> concepts) {

        return result -> {
            if (Files.notExists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            for (Integer id : concepts) {
                Path fileName = targetDir.resolve(String.format(filenamePattern, id));
                if (result.containsKey(id)) {
                    toFile(fileName).write(result.get(id));
                }
            }
        };
    }
}
