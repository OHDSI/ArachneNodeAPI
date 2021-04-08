/*
 *
 * Copyright 2020 Odysseus Data Services, inc.
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
 * Authors: Alex Cumarav, Vitaliy Kulakov, Yaroslav Molodkov
 * Created: July 30, 2020
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFile;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFileType;
import com.odysseusinc.arachne.datanode.repository.AnalysisFileRepository;
import com.odysseusinc.arachne.datanode.repository.AnalysisRepository;
import com.odysseusinc.arachne.datanode.service.AnalysisResultsService;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.odysseusinc.arachne.datanode.Constants.Analysis.ERROR_REPORTR_FILENAME;
import static com.odysseusinc.arachne.datanode.Constants.Analysis.ERROR_REPORT_FILENAME;
import static com.odysseusinc.arachne.datanode.Constants.AnalysisMessages.ANALYSIS_IS_NOT_EXISTS_LOG;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;

@Service
@Transactional
public class AnalysisResultsServiceImpl implements AnalysisResultsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisResultsServiceImpl.class);

    private final AnalysisFileRepository analysisFileRepository;
    private final AnalysisRepository analysisRepository;

    @Autowired
    public AnalysisResultsServiceImpl(AnalysisFileRepository analysisFileRepository, AnalysisRepository analysisRepository) {

        this.analysisFileRepository = analysisFileRepository;
        this.analysisRepository = analysisRepository;
    }

    @Override
    public List<AnalysisFile> getAnalysisResults(Analysis analysis) {

        return analysisFileRepository.findAllByAnalysisIdAndType(
                analysis.getId(),
                AnalysisFileType.ANALYSYS_RESULT);
    }

    @Override
    public Analysis saveResults(Analysis analysis, File resultDir) {

        List<AnalysisFile> resultFiles = Arrays.stream(resultDir.listFiles())
                .map(file -> new AnalysisFile(file.getAbsolutePath(), AnalysisFileType.ANALYSYS_RESULT, analysis))
                .collect(Collectors.toList());
        analysisFileRepository.save(resultFiles);
        return updateAnalysisWithResultsData(analysis, resultDir);
    }

    private Analysis updateAnalysisWithResultsData(Analysis analysis, File resultDir) {

        Analysis exists = analysisRepository.findOne(analysis.getId());
        if (exists == null) {
            LOGGER.warn(ANALYSIS_IS_NOT_EXISTS_LOG, analysis.getId());
            return null;
        }
        removeAnalysisFolder(exists);
        final AnalysisResultStatusDTO updatedAnalysisStatus = reEvaluateAnalysisStatus(analysis.getStatus(), resultDir);
        exists.setAnalysisFolder(resultDir.getAbsolutePath());
        exists.setStatus(updatedAnalysisStatus);
        exists.setStdout(analysis.getStdout());
        exists.getStateHistory().addAll(analysis.getStateHistory());
        return analysisRepository.save(exists);
    }

    private void removeAnalysisFolder(Analysis exists) {

        File analysisFolder = new File(exists.getAnalysisFolder());
        try {
            FileUtils.deleteDirectory(analysisFolder);
        } catch (IOException e) {
            LOGGER.warn(Constants.AnalysisMessages.CANT_REMOVE_ANALYSIS_DIR_LOG);
        }
    }

    private AnalysisResultStatusDTO reEvaluateAnalysisStatus(AnalysisResultStatusDTO originalStatus, File resultDir) {

        if (AnalysisResultStatusDTO.EXECUTED == originalStatus) {
            if (resultDir == null ) {
                LOGGER.error("Result directory cannot be null");
                return AnalysisResultStatusDTO.FAILED;
            }
            File[] zipFiles = resultDir.listFiles((dir, name) -> name.endsWith(".zip"));
            if (checkZipArchiveForErrorFile(zipFiles)) {
                LOGGER.warn("Unexpected errorReport file found. Changing analysis status to FAILED for {}", resultDir);
                return AnalysisResultStatusDTO.FAILED;
            }
        }
        return originalStatus;
    }

    private boolean checkZipArchiveForErrorFile(File[] listFiles) {

        return Stream.of(listFiles)
                .map(this::scanZipForErrorFilenames)
                .reduce(Boolean::logicalOr)
                .orElse(false);
    }

    private boolean scanZipForErrorFilenames(File zipFile) {

        try (ZipFile archive = new ZipFile(zipFile)) {
            return archive.stream()
                    .map(ZipEntry::getName)
                    .anyMatch(name -> endsWithIgnoreCase(name, ERROR_REPORT_FILENAME) || endsWithIgnoreCase(name, ERROR_REPORTR_FILENAME));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
