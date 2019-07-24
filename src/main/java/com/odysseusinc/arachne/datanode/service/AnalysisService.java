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

package com.odysseusinc.arachne.datanode.service;

import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFile;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFileStatus;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.lingala.zip4j.exception.ZipException;
import org.springframework.web.multipart.MultipartFile;

public interface AnalysisService {

    Integer invalidateAllUnfinishedAnalyses(final User user);

    List<AnalysisFile> getAnalysisResults(Analysis analysis);

    List<AnalysisFile> getAnalysisResults(Analysis analysis, AnalysisFileStatus status);

    Optional<Analysis> findAnalysis(Long id);

    void sendToEngine(Analysis analysis);

    Analysis saveResults(Analysis analysis, File resultDir);

    Analysis persist(Analysis analysis);

    Optional<Analysis> updateStatus(Long id, String stdoutDiff, String password);

    void invalidateExecutingLong();

    void saveAnalysisFiles(Analysis analysis, List<MultipartFile> files) throws IOException, ZipException;
}
