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

package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.exception.ArachneSystemRuntimeException;
import com.odysseusinc.arachne.datanode.exception.ValidationException;
import com.odysseusinc.arachne.datanode.service.ExecutionEngineIntegrationService;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.util.CommonFileUtils;
import com.odysseusinc.arachne.datanode.service.client.engine.EngineClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExecutionEngineIntegrationServiceImpl implements ExecutionEngineIntegrationService {

    private final EngineClient engineClient;

    @Autowired
    public ExecutionEngineIntegrationServiceImpl(EngineClient engineClient) {

        this.engineClient = engineClient;
    }

    @Override
    public AnalysisRequestStatusDTO sendAnalysisRequest(AnalysisRequestDTO requestDTO,
                                                                        File analysisFolder, boolean compressedResult) {

        return sendAnalysisRequest(requestDTO, analysisFolder, compressedResult, false);
    }

    @Override
    public AnalysisRequestStatusDTO sendAnalysisRequest(AnalysisRequestDTO requestDTO,
                                                                        File analysisFolder, boolean compressedResult,
                                                                        boolean healthCheck) {

        final File analysisTempDir = getTempDirectory("arachne_datanode_analysis_");
        try {
            final File archive = new File(analysisTempDir.toString(), "request.zip");
            CommonFileUtils.compressAndSplit(analysisFolder, archive, null);

            try (InputStream in = new FileInputStream(archive)) {
                MultipartFile file = new MockMultipartFile(archive.getName(), archive.getName(), MediaType.APPLICATION_OCTET_STREAM_VALUE, in);
                return engineClient.sendAnalysisRequest(requestDTO, file,
                        compressedResult, healthCheck);
            }
        } catch (ResourceAccessException exception) {
            throw new ValidationException("Cannot establish connection to the execution engine");
        } catch (ZipException | IOException zipException) {
            throw new ArachneSystemRuntimeException(zipException.getMessage());
        } finally {
            FileUtils.deleteQuietly(analysisTempDir);
        }
    }

    private File getTempDirectory(String prefix) {

        try {
            return Files.createTempDirectory(prefix).toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
