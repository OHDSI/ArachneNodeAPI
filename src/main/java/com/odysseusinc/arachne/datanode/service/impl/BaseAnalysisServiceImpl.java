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

import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.environment.EnvironmentDescriptorService;
import com.odysseusinc.arachne.datanode.exception.ArachneSystemRuntimeException;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFile;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFileStatus;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFileType;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisState;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisStateEntry;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.repository.AnalysisRepository;
import com.odysseusinc.arachne.datanode.repository.AnalysisStateJournalRepository;
import com.odysseusinc.arachne.datanode.service.AnalysisService;
import com.odysseusinc.arachne.datanode.service.ExecutionEngineIntegrationService;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.util.CommonFileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

public abstract class BaseAnalysisServiceImpl implements AnalysisService {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseAnalysisServiceImpl.class);
    private static List<String> finishedStates = new ArrayList<>(3);
    private static final String ZIP_FILENAME = "analysis.zip";

    static {
        finishedStates.add(AnalysisState.EXECUTED.name());
        finishedStates.add(AnalysisState.CLOSED.name());
        finishedStates.add(AnalysisState.DEAD.name());
    }

    protected final GenericConversionService conversionService;
    protected final AnalysisPreprocessorService preprocessorService;
    protected final AnalysisRepository analysisRepository;
    protected final AnalysisStateJournalRepository analysisStateJournalRepository;
    private final ExecutionEngineIntegrationService engineIntegrationService;
    @Autowired
    private EnvironmentDescriptorService environmentService;
    @Value("${datanode.arachneCentral.host}")
    protected String centralHost;
    @Value("${datanode.arachneCentral.port}")
    protected Integer centralPort;
    @Value("${analysis.scheduler.invalidateExecutingInterval}")
    protected Long invalidateExecutingInterval;
    @Value("${analysis.scheduler.invalidateMaxDaysExecutingInterval}")
    protected Integer invalidateMaxDaysExecutingInterval;
    @Value("${analysis.file.maxsize}")
    protected Long maximumSize;
    @Value("${submission.result.files.exclusions}")
    private String resultExclusions;

    @Autowired
    public BaseAnalysisServiceImpl(GenericConversionService conversionService,
                                   AnalysisPreprocessorService preprocessorService,
                                   AnalysisRepository analysisRepository,
                                   AnalysisStateJournalRepository analysisStateJournalRepository,
                                   ExecutionEngineIntegrationService engineIntegrationService) {

        this.analysisRepository = analysisRepository;
        this.analysisStateJournalRepository = analysisStateJournalRepository;
        this.conversionService = conversionService;
        this.engineIntegrationService = engineIntegrationService;
        this.preprocessorService = preprocessorService;
    }

    @Override
    @Transactional
    public Integer invalidateAllUnfinishedAnalyses(final User user) {

        List<Analysis> unfinished = analysisRepository.findAllByNotStateIn(finishedStates);
        List<AnalysisStateEntry> entries = onAnalysesInvalidated(unfinished, user);
        analysisStateJournalRepository.saveAll(entries);
        return unfinished.size();
    }

    protected List<AnalysisStateEntry> onAnalysesInvalidated(List<Analysis> unfinished, final User user) {

		List<AnalysisStateEntry> entries = new LinkedList<>();
		unfinished.forEach(analysis -> {
			analysis.setStatus(AnalysisResultStatusDTO.FAILED);
			AnalysisStateEntry entry = new AnalysisStateEntry(
					new Date(),
					AnalysisState.CLOSED,
					"Invalidated by user's request",
					analysis
			);
			entries.add(entry);
		});
		return entries;
	}

    @Async
    @Transactional
    public void sendToEngine(Analysis analysis) {

        preprocessorService.runPreprocessor(analysis);
        AnalysisRequestDTO analysisRequestDTO = conversionService.convert(analysis, AnalysisRequestDTO.class);
        analysisRequestDTO.setResultExclusions(resultExclusions);
        File analysisFolder = new File(analysis.getAnalysisFolder());
        AnalysisState state;
        String reason;
        Long id = analysis.getId();
        try {
            AnalysisRequestStatusDTO exchange = engineIntegrationService.sendAnalysisRequest(analysisRequestDTO, analysisFolder, true);
            LOGGER.info("Request [{}] of type [{}] sent successfully", id, exchange.getType());
            String descriptorId = exchange.getActualDescriptorId();
            LOGGER.info("Request [{}] of type [{}] sent successfully, descriptor in use [{}]", id, exchange.getType(), descriptorId);
            analysis.setActualEnvironment(Optional.ofNullable(descriptorId).flatMap(environmentService::byDescriptorId).orElse(null));
            reason = String.format(Constants.AnalysisMessages.SEND_REQUEST_TO_ENGINE_SUCCESS_REASON, id, exchange.getType());
            state = AnalysisState.EXECUTING;
        } catch (RestClientException | ArachneSystemRuntimeException e) {
            reason = String.format(Constants.AnalysisMessages.SEND_REQUEST_TO_ENGINE_FAILED_REASON,
                    id,
                    e.getMessage());
            LOGGER.info("Request [{}] failed with [{}]: {}", id, e.getClass(), e.getMessage());
            state = AnalysisState.EXECUTION_FAILURE;
        }
        updateState(analysis, state, reason);
    }

    protected void updateState(Analysis analysis, AnalysisState state, String reason) {

        AnalysisStateEntry analysisStateEntry = new AnalysisStateEntry(new Date(), state, reason, analysis);
        analysisStateJournalRepository.save(analysisStateEntry);
    }

    @Transactional
    public Analysis persist(Analysis analysis) {

        Analysis exists = Objects.nonNull(analysis.getId()) ? analysisRepository.getOne(analysis.getId()) : null;
		if (exists == null) {
			LOGGER.debug("Analysis with id: '{}' is not exist. Saving...", analysis.getId());
			return analysisRepository.save(analysis);
		} else {
			return exists;
		}
    }

    public Optional<Analysis> updateStatus(Long id, String stdoutDiff, String password) {

        final Date currentTime = new Date();
        final Optional<Analysis> optionalAnalysis = analysisRepository.findOneExecuting(id, password);
        optionalAnalysis
                .ifPresent(analysis -> {
                    analysisStateJournalRepository.findLatestByAnalysisId(analysis.getId())
                            .ifPresent(currentState -> {
                                if (AnalysisState.EXECUTING == currentState.getState()) {
                                    final String stdout = analysis.getStdout();
                                    analysis.setStdout(stdout == null ? stdoutDiff : stdout + stdoutDiff);
                                    analysisRepository.save(analysis);
                                    AnalysisStateEntry analysisStateEntry = new AnalysisStateEntry(
                                            currentTime,
                                            AnalysisState.EXECUTING,
                                            Constants.AnalysisMessages.STDOUT_UPDATED_REASON,
                                            analysis);
                                    analysisStateEntry.setId(currentState.getId());
                                    analysisStateJournalRepository.save(analysisStateEntry);
                                }
                            });
                });
        return optionalAnalysis;
    }

    @Transactional
    public void invalidateExecutingLong() {

        Date resendBefore = new Date(new Date().getTime() - invalidateExecutingInterval);
        List<Analysis> allExecutingMoreThan = analysisRepository.findAllExecutingMoreThan(
                AnalysisState.EXECUTING.name(),
                resendBefore
        );
        List<AnalysisStateEntry> entries = new ArrayList<>();
        Date expirationDate = calculateDate(invalidateMaxDaysExecutingInterval);
        allExecutingMoreThan.forEach(analysis -> {
            LOGGER.warn("EXECUTING State being invalidated for analysis with id='{}'", analysis.getId());
            Optional<AnalysisStateEntry> state = analysisStateJournalRepository.findLatestByAnalysisId(analysis.getId());
            state.ifPresent(s -> {
                AnalysisState analysisState = AnalysisState.EXECUTION_FAILURE;
                if (s.getDate().before(expirationDate)) {
                    LOGGER.warn("Analysis id={} is being EXECUTING more than {} days, one will marked as DEAD",
                            analysis.getId(), invalidateMaxDaysExecutingInterval);
                    analysisState = AnalysisState.DEAD;
                }
                AnalysisStateEntry entry = new AnalysisStateEntry(
                        new Date(),
                        analysisState,
                        "Analysis sent to Execution Engine early than " + resendBefore,
                        analysis
                );
                entries.add(entry);
            });
        });
        analysisStateJournalRepository.saveAll(entries);
    }

    private Date calculateDate(int interval) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -interval);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @Override
    @Transactional
    public Optional<Analysis> findAnalysis(Long id) {

        return analysisRepository.findById(id);
    }

    @Override
    public void saveAnalysisFiles(Analysis analysis, List<MultipartFile> files) throws IOException {

        final File analysisDir = new File(analysis.getAnalysisFolder());
        final File zipDir = Paths.get(analysisDir.getPath(), Constants.Analysis.SUBMISSION_ARCHIVE_SUBDIR).toFile();
        FileUtils.forceMkdir(zipDir);

        try {
            if (files.size() == 1) { // single file can be zipped archive
                MultipartFile archive = files.stream().findFirst().get();
                File archiveFile = new File(zipDir, ZIP_FILENAME);
                archive.transferTo(archiveFile);
                CommonFileUtils.unzipFiles(archiveFile, analysisDir);
            } else {
                files.forEach(f -> {
                    try {
                        f.transferTo(new File(analysisDir, f.getOriginalFilename()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            File[] filesList = analysisDir.listFiles();

            if (Objects.nonNull(filesList)) {
                List<AnalysisFile> analysisFiles = Arrays.stream(filesList)
                        .filter(File::isFile)
                        .map(f -> {
                            AnalysisFile analysisFile = new AnalysisFile();
                            analysisFile.setAnalysis(analysis);
                            analysisFile.setType(AnalysisFileType.ANALYSIS);
                            analysisFile.setStatus(AnalysisFileStatus.UNPROCESSED);
                            analysisFile.setLink(f.getPath());
                            return analysisFile;
                        }).collect(Collectors.toList());
                analysis.setAnalysisFiles(analysisFiles);
            }
        } finally {
            FileUtils.deleteQuietly(zipDir);
        }
    }
}
