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
import com.odysseusinc.arachne.datanode.service.ExecutionEngineStatus;
import com.odysseusinc.arachne.datanode.service.client.engine.ExecutionEngineClient;
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
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

import static com.odysseusinc.arachne.datanode.service.ExecutionEngineStatus.OFFLINE;
import static com.odysseusinc.arachne.datanode.service.ExecutionEngineStatus.ONLINE;

@Service
public class ExecutionEngineIntegrationServiceImpl implements ExecutionEngineIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionEngineIntegrationServiceImpl.class);

    @Autowired
    private ExecutionEngineClient engineClient;
    private final EngineClient engineStatusClient;

    private volatile ExecutionEngineStatus executionEngineStatus = OFFLINE;

    @Autowired
    public ExecutionEngineIntegrationServiceImpl(@Qualifier("engineStatusClient") EngineClient engineStatusClient) {
        this.engineStatusClient = engineStatusClient;
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
            logger.info("Request [{}} with files for [{}], sending now", requestDTO.getId(), analysisFolder.getName());
            return engineClient.sendAnalysisRequest(requestDTO, archive, compressedResult, healthCheck);
        } catch (ResourceAccessException exception) {
            throw new ValidationException("Cannot establish connection to the execution engine");
        } catch (IOException zipException) {
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

    @Scheduled(fixedDelayString = "${executionEngine.status.period}")
    public void checkStatus() {
        try {
            engineStatusClient.checkStatus();
            
            if (OFFLINE.equals(this.executionEngineStatus)) {
                logger.info("Execution engine is online");
            }
            executionEngineStatus = ONLINE;
        } catch (Exception e) {
            if (ONLINE.equals(this.executionEngineStatus)) {
                logger.info("Execution engine is offline");
            }
            executionEngineStatus = OFFLINE;
        }
    }

    @Override
    public ExecutionEngineStatus getExecutionEngineStatus() {
        return this.executionEngineStatus;
    }
}
