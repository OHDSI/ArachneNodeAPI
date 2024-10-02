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
 * Created: May 18, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAchillesReportDTO;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.config.properties.AchillesProperties;
import com.odysseusinc.arachne.datanode.exception.AchillesJobInProgressException;
import com.odysseusinc.arachne.datanode.exception.AchillesResultNotAvailableException;
import com.odysseusinc.arachne.datanode.exception.ArachneSystemRuntimeException;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJob;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJobSource;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus;
import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.repository.AchillesJobRepository;
import com.odysseusinc.arachne.datanode.service.AchillesService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors;
import com.odysseusinc.arachne.datanode.service.achilles.ConditionEraReport;
import com.odysseusinc.arachne.datanode.service.achilles.ConditionReport;
import com.odysseusinc.arachne.datanode.service.achilles.DashboardReport;
import com.odysseusinc.arachne.datanode.service.achilles.DataDensityReport;
import com.odysseusinc.arachne.datanode.service.achilles.DeathReport;
import com.odysseusinc.arachne.datanode.service.achilles.DrugEraReport;
import com.odysseusinc.arachne.datanode.service.achilles.DrugReport;
import com.odysseusinc.arachne.datanode.service.achilles.MeasurementReport;
import com.odysseusinc.arachne.datanode.service.achilles.ObservationPeriodReport;
import com.odysseusinc.arachne.datanode.service.achilles.ObservationReport;
import com.odysseusinc.arachne.datanode.service.achilles.PersonReport;
import com.odysseusinc.arachne.datanode.service.achilles.ProcedureReport;
import com.odysseusinc.arachne.datanode.service.achilles.VisitReport;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.util.CentralUtil;
import com.odysseusinc.arachne.datanode.util.DataSourceUtils;
import com.odysseusinc.arachne.datanode.util.SqlUtils;
import com.odysseusinc.arachne.datanode.util.datasource.ResultSetContainer;
import com.odysseusinc.arachne.datanode.util.datasource.ResultSetProcessor;
import com.odysseusinc.arachne.datanode.util.datasource.ResultTransformers;
import com.odysseusinc.arachne.datanode.util.datasource.ResultWriters;
import com.odysseusinc.arachne.execution_engine_common.util.CommonFileUtils;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.odysseusinc.arachne.datanode.Constants.Achilles.*;
import static com.odysseusinc.arachne.datanode.Constants.CDM.CONCEPT_ID;
import static com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus.*;
import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.resultSet;
import static com.odysseusinc.arachne.datanode.util.datasource.QueryProcessors.statement;
import static java.lang.String.valueOf;

@Service
public class AchillesServiceImpl implements AchillesService {

    public static final String ACHILLES_HEEL_SQL = "classpath:/achilles/data/export/achillesheel/sqlAchillesHeel.sql";
    public static final String DRUG_ERA_SQL = "classpath:/achilles/data/export/drugera/sqlDrugEraTreemap.sql";
    public static final String DRUG_TREEMAP_SQL = "classpath:/achilles/data/export/drug/sqlDrugTreemap.sql";
    public static final String CONDITION_TREEMAP_SQL = "classpath:/achilles/data/export/condition/sqlConditionTreemap.sql";
    public static final String CONDITION_ERA_TREEMAP_SQL = "classpath:/achilles/data/export/conditionera/sqlConditionEraTreemap.sql";
    public static final String PROCEDURE_TREEMAP_SQL = "classpath:/achilles/data/export/procedure/sqlProcedureTreemap.sql";
    public static final String MEASUREMENT_TREEMAP_SQL = "classpath:/achilles/data/export/measurement/sqlMeasurementTreemap.sql";
    public static final String OBSERVATION_TREEMAP_SQL = "classpath:/achilles/data/export/observation/sqlObservationTreemap.sql";
    public static final String VISIT_SQL = "classpath:/achilles/data/export/visit/sqlVisitTreemap.sql";
    public static final String ACHILLES_RESULTS_EXCEPTION = "Achilles results is not available, table %s was not found";
    protected static final Logger LOGGER = LoggerFactory.getLogger(AchillesServiceImpl.class);
    protected static final String DATA_NODE_NOT_EXISTS_EXCEPTION = "DataNode is not registered, please register";
    private static final String ACHILLES_RESULTS_AVAILABLE_LOG = "Achilles results available at: {}";
    protected final DockerClientConfig dockerClientConfig;
    protected final AchillesProperties properties;
    protected final CentralUtil centralUtil;
    protected final RetryTemplate retryTemplate;
    protected final DataNodeService dataNodeService;
    protected final AchillesJobRepository achillesJobRepository;
    @Value("${datanode.arachneCentral.host}")
    protected String centralHost;
    @Value("${datanode.arachneCentral.port}")
    protected Integer centralPort;

    @Value("${tmp.location-on-host:}")
    protected String tmpLocationOnHost;

    @Autowired
    protected ApplicationContext applicationContext;
    @Autowired
    protected SqlUtils sqlUtils;
    @Autowired
    protected ConditionEraReport conditionEraReport;
    @Autowired
    protected ConditionReport conditionReport;
    @Autowired
    protected DrugEraReport drugEraReport;
    @Autowired
    protected DrugReport drugReport;
    @Autowired
    protected ProcedureReport procedureReport;
    @Autowired
    protected PersonReport personReport;
    @Autowired
    protected DeathReport deathReport;
    @Autowired
    protected ObservationPeriodReport observationPeriodReport;
    @Autowired
    protected DashboardReport dashboardReport;
    @Autowired
    protected DataDensityReport dataDensityReport;
    @Autowired
    protected MeasurementReport measurementReport;
    @Autowired
    protected ObservationReport observationReport;
    @Autowired
    protected VisitReport visitReport;
    @Autowired
    protected CentralSystemClient centralSystemClient;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    public AchillesServiceImpl(DockerClientConfig dockerClientConfig,
                               AchillesProperties properties,
                               CentralUtil centralUtil,
                               @Qualifier("achillesRetryTemplate") RetryTemplate retryTemplate,
                               DataNodeService dataNodeService,
                               AchillesJobRepository achillesJobRepository) {

        this.dockerClientConfig = dockerClientConfig;
        this.properties = properties;
        this.centralUtil = centralUtil;
        this.retryTemplate = retryTemplate;
        this.dataNodeService = dataNodeService;
        this.achillesJobRepository = achillesJobRepository;
    }

    @PostConstruct
    public void init() {

        List<AchillesJob> jobs = achillesJobRepository.findByStatus(AchillesJobStatus.IN_PROGRESS)
                .stream()
                .peek(job -> job.setStatus(AchillesJobStatus.FAILED))
                .collect(Collectors.toList());
        achillesJobRepository.saveAll(jobs);
    }

    @Async
    @Override
    public void executeAchilles(DataSource dataSource) {

        LOGGER.info("Starting achilles for: {}", dataSource);
        AchillesJob job = checkAchillesJob(dataSource, AchillesJobSource.GENERATION);
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("achilles_");
            Path results = runAchilles(dataSource, job, workDir);
            updateJob(job, SUCCESSFUL);
            System.out.println("Achilles generation finished");
            try{
                AchillesJob importAchillesJob = createAchillesImportJob(dataSource);
                if (importAchillesJob != null) {
                    pullAchillesData(importAchillesJob);
                }
                updateJob(importAchillesJob, SUCCESSFUL);
            }catch(Exception ex){
                System.out.println("Coulnd't import achilles results: "+ex);
            }



        } catch (Exception e) {
            System.out.println(" Achilles failed to execute "+ e);
            updateJob(job, FAILED);
        } finally {
            if (workDir != null && !LOGGER.isDebugEnabled()) {
                FileUtils.deleteQuietly(workDir.toFile());
            }
        }
    }

    private AchillesJob checkAchillesJob(DataSource dataSource, AchillesJobSource source) {

        achillesJobRepository.findTopByDataSourceAndStatusOrderByStarted(dataSource, IN_PROGRESS)
                .ifPresent(job -> {
                    final String message = String.format("Achilles is in progress for datasource: %s", dataSource);
                    throw new AchillesJobInProgressException(message);
                });
        return createJob(dataSource, source);
    }

    @Override
    public boolean hasAchillesResultTable(DataSource dataSource) {

        try {
            String query = "select count(*) from %s.achilles_results";
            query = String.format(query, getResultSchema(dataSource));
            Map<String, Integer> result = DataSourceUtils.<Integer>withDataSource(dataSource)
                    .ifTableNotExists(dataSource.getResultSchema(), "achilles_results",
                            table -> new AchillesResultNotAvailableException(String.format(ACHILLES_RESULTS_EXCEPTION, table)))
                    .run(statement(query))
                    .collectResults(resultSet -> {
                        Map<String, Integer> data = new HashMap<>();
                        int count = 0;
                        if (resultSet.next()) {
                            count = resultSet.getInt(1);
                        }
                        data.put("count", count);
                        return new ResultSetContainer<>(data, null);
                    })
                    .getResults();
            return result.getOrDefault("count", 0) > 0;
        } catch (SQLException e) {
            LOGGER.warn("Failed to check achilles results", e);
            return false;
        } catch (AchillesResultNotAvailableException e) {
            LOGGER.info(e.getMessage());
            return false;
        }
    }

    @Override
    public AchillesJob createAchillesImportJob(DataSource dataSource) {

        if (hasAchillesResultTable(dataSource)) {
            return checkAchillesJob(dataSource, AchillesJobSource.IMPORT);
        }
        return null;
    }

    @Async
    @Override
    public void pullAchillesData(AchillesJob job) {

        DataSource dataSource = job.getDataSource();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Start pulling Achilles data for {}", dataSource);
        }
        try {
            Path tempDir = Files.createTempDirectory("achilles_");
            ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setDaemon(false)
                    .setThreadFactory(Executors.defaultThreadFactory())
                    .setNameFormat("achilles-pool-%d")
                    .build();
            ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);
            try {
                List<Callable<String>> tasks = buildReportTasks(dataSource, tempDir);

                try {
                    List<Future<String>> futures = executorService.invokeAll(tasks);
                    for (Future<String> future : futures) {
                        String taskResultInfo = future.get();
                        LOGGER.info(taskResultInfo);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.warn("Achilles pull interrupted", e);
                    throw new RuntimeException("Achilles pull interrupted", e);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Achilles data has collected at {}", tempDir);
                }
                retryTemplate.execute((RetryCallback<Void, Exception>) retryContext -> {

                    sendResultToCentral(dataSource, tempDir);
                    return null;
                });
                updateJob(job, SUCCESSFUL);
            } finally {
                executorService.shutdown();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to pull achilles results", e);
            updateJob(job, FAILED);
        }
    }

    private List<Callable<String>> buildReportTasks(DataSource dataSource, Path tempDir) {
        List<Callable<String>> tasks = new ArrayList<>();

        tasks.add(achillesTask("Heel", () -> runAchillesQuery(dataSource, ACHILLES_HEEL_SQL, tempDir.resolve("achillesheel.json"),
                AchillesProcessors.achillesHeel())));

        tasks.add(achillesTask("DrugEra", () -> {
            List<Integer> drugEraConcepts = new LinkedList<>();
            Integer result = runAchillesQuery(dataSource, DRUG_ERA_SQL, tempDir.resolve("drugera_treemap.json"),
                    resultSet(),
                    transmitToList(CONCEPT_ID, drugEraConcepts));
            result += drugEraReport.runReports(dataSource, tempDir.resolve("drugeras"), drugEraConcepts);
            return result;
        }));

        tasks.add(achillesTask("Drugs", () -> {
            List<Integer> drugConcepts = new LinkedList<>();
            Integer result = runAchillesQuery(dataSource, DRUG_TREEMAP_SQL, tempDir.resolve("drug_treemap.json"),
                    resultSet(),
                    transmitToList(CONCEPT_ID, drugConcepts));
            result += drugReport.runReports(dataSource, tempDir.resolve("drugs"), drugConcepts);
            return result;
        }));

        tasks.add(achillesTask("Conditions", () -> {
            List<Integer> conditionConcepts = new LinkedList<>();
            Integer result = runAchillesQuery(dataSource, CONDITION_TREEMAP_SQL, tempDir.resolve("condition_treemap.json"),
                    resultSet(),
                    transmitToList(CONCEPT_ID, conditionConcepts));
            result += conditionReport.runReports(dataSource, tempDir.resolve("conditions"), conditionConcepts);
            return result;
        }));

        tasks.add(achillesTask("ConditionEra", () -> {
            List<Integer> conditionEraConcepts = new LinkedList<>();
            Integer result = runAchillesQuery(dataSource, CONDITION_ERA_TREEMAP_SQL, tempDir.resolve("conditionera_treemap.json"),
                    resultSet(),
                    transmitToList(CONCEPT_ID, conditionEraConcepts));
            result += conditionEraReport.runReports(dataSource, tempDir.resolve("conditioneras"), conditionEraConcepts);
            return result;
        }));

        tasks.add(achillesTask("Procedure", () -> {
            List<Integer> procedureConcepts = new LinkedList<>();
            Integer result = runAchillesQuery(dataSource, PROCEDURE_TREEMAP_SQL, tempDir.resolve("procedure_treemap.json"),
                    resultSet(),
                    transmitToList(CONCEPT_ID, procedureConcepts));
            result += procedureReport.runReports(dataSource, tempDir.resolve("procedures"), procedureConcepts);
            return result;
        }));

        tasks.add(achillesTask("Person", () -> personReport.runReports(dataSource, tempDir.resolve("person.json"), null)));

        tasks.add(achillesTask("Death", () -> deathReport.runReports(dataSource, tempDir.resolve("death.json"), null)));

        tasks.add(achillesTask("Dashboard", () -> dashboardReport.runReports(dataSource, tempDir.resolve("dashboard.json"), null)));
        tasks.add(achillesTask("DataDensity", () -> dataDensityReport.runReports(dataSource, tempDir.resolve("datadensity.json"), null)));

        tasks.add(achillesTask("Measurement", () -> {
            List<Integer> measurementConcepts = new LinkedList<>();
            Integer result = runAchillesQuery(dataSource, MEASUREMENT_TREEMAP_SQL, tempDir.resolve("measurement_treemap.json"),
                    resultSet(),
                    transmitToList(CONCEPT_ID, measurementConcepts));
            result += measurementReport.runReports(dataSource, tempDir.resolve("measurements"), measurementConcepts);
            return result;
        }));

        tasks.add(achillesTask("Observation", () -> {
            List<Integer> observationConcepts = new LinkedList<>();
            Integer result = runAchillesQuery(dataSource, OBSERVATION_TREEMAP_SQL, tempDir.resolve("observation_treemap.json"),
                    resultSet(),
                    transmitToList(CONCEPT_ID, observationConcepts));
            result += observationReport.runReports(dataSource, tempDir.resolve("observations"), observationConcepts);
            return result;
        }));
        tasks.add(achillesTask("Visit", () -> {
            List<Integer> visitConcepts = new LinkedList<>();
            Integer result = runAchillesQuery(dataSource, VISIT_SQL, tempDir.resolve("visit_treemap.json"),
                    resultSet(),
                    transmitToList(CONCEPT_ID, visitConcepts));
            result += visitReport.runReports(dataSource, tempDir.resolve("visits"), visitConcepts);
            return result;
        }));

        return tasks;
    }

    private String getResultSchema(DataSource dataSource) {

        return StringUtils.defaultIfEmpty(dataSource.getResultSchema(), dataSource.getCdmSchema());
    }

    private Callable<String> achillesTask(String name, Callable<Integer> callable) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("mm:ss.SSS");
        return () -> {
            LocalTime start = LocalTime.now();
            Integer records = callable.call();
            Duration duration = Duration.between(start, LocalTime.now());
            String fd = formatter.format(LocalTime.MIDNIGHT.plus(duration));
            return String.format("Task %s proceed %d record(s) completed in %s", name, records, fd);
        };
    }

    private Consumer<Map> transmitToList(String key, List<Integer> identities) {

        Objects.requireNonNull(identities);
        return results -> {
            List conceptIds = (List) results.getOrDefault(key, new ArrayList<>());
            identities.addAll(conceptIds);
        };
    }

    private Integer runAchillesQuery(DataSource dataSource, String queryPath, Path targetPath,
                                     ResultSetProcessor<Map> processor) throws IOException, SQLException {

        return runAchillesQuery(dataSource, queryPath, targetPath, processor, null);
    }

    private Integer runAchillesQuery(DataSource dataSource, String queryPath, Path targetPath,
                                     ResultSetProcessor<Map> processor,
                                     Consumer<Map> transmitter)
            throws SQLException, IOException {

        String query = sqlUtils.transformSqlTemplate(dataSource, queryPath);
        return DataSourceUtils.<String>withDataSource(dataSource)
                .run(statement(query))
                .collectResults(processor)
                .transmitResults(transmitter)
                .transform(ResultTransformers.toJson())
                .write(ResultWriters.toFile(targetPath))
                .getResultsCount();
    }

    private AchillesJob createJob(DataSource dataSource, AchillesJobSource source) {

        AchillesJob job = new AchillesJob();
        job.setDataSource(dataSource);
        job.setStarted(new Date());
        job.setStatus(IN_PROGRESS);
        job.setSource(source);
        return achillesJobRepository.save(job);
    }

    private void updateJob(AchillesJob job, AchillesJobStatus status) {

        job.setFinished(new Date());
        job.setStatus(status);
        achillesJobRepository.save(job);
    }

    @Override
    public List<CommonAchillesReportDTO> getAchillesReports() {

        return centralSystemClient.listReports();
    }

    private void sendResultToCentral(DataSource dataSource, Path results) throws IOException {

        LOGGER.debug("Sending results to central: {}", centralHost);
        final File file = results.toFile();
        final File targetFile = new File("/tmp", "archive" + UUID.randomUUID());
        try {
            CommonFileUtils.compressAndSplit(file, targetFile, null);

            FileInputStream input = new FileInputStream(targetFile);
            MultipartFile multipartFile = new MockMultipartFile("file",
                    file.getName(), "text/plain", IOUtils.toByteArray(input));

            centralSystemClient.sendAchillesResults(dataSource.getCentralId(), multipartFile);

            LOGGER.debug("Results successfully sent");

        } catch (ZipException zipException) {
            throw new ArachneSystemRuntimeException(zipException.getMessage());
        } finally {
            FileUtils.deleteQuietly(targetFile);
        }
    }

    private String getTempLocationOnHost(Path workDir) {
        if (StringUtils.isEmpty(tmpLocationOnHost)) {
            return workDir.toString();
        }
        return workDir.toString().replaceFirst(
                "^" + Pattern.quote(System.getProperty("java.io.tmpdir")),
                Matcher.quoteReplacement(tmpLocationOnHost)
        );
    }

    private DockerClient createDockerClient(DockerClientConfig dockerClientConfig) {
        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .sslConfig(dockerClientConfig.getSSLConfig())
                .build();
        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }

    private Path runAchilles(final DataSource dataSource, AchillesJob job, Path workDir) throws InterruptedException, IOException {

        LOGGER.debug("Achilles result directory: {}", workDir);
        String imageName = properties.getImageName();
        List<String> logEntries = new LinkedList<>();

        try (DockerClient dockerClient = createDockerClient(this.dockerClientConfig)) {
            pullAchillesImage(dockerClient, imageName);
            CreateContainerResponse container = buildContainer(dockerClient, dataSource, workDir, imageName);
            String containerId = container.getId();
            LOGGER.debug("Starting container: {}", containerId);
            Integer statusCode = executeContainer(dockerClient, containerId, logEntries);
            dockerClient.removeContainerCmd(containerId)
                    .withRemoveVolumes(!LOGGER.isDebugEnabled())
                    .exec();

            LOGGER.debug("Achilles finished with status code: {}", statusCode);
            if (statusCode == 0) {
                Path jsonDir = createAchillesOutputResults(dataSource, workDir);
                LOGGER.info(ACHILLES_RESULTS_AVAILABLE_LOG, jsonDir);
                return jsonDir;
            } else {
                logAchillesError(dataSource, workDir, logEntries, dockerClient, containerId);
                throw new IOException("Achilles exited with errors, lost results");
            }

        } finally {
            job.setAchillesLog(logEntries.stream().map(s -> s.replaceAll("\u0000", "")).collect(Collectors.joining("\n")));
            achillesJobRepository.save(job);
        }
    }

    private void logAchillesError(DataSource dataSource, Path workDir, List<String> logEntries, DockerClient dockerClient, String containerId) throws IOException {
        final CopyArchiveFromContainerCmd errorReport
                = dockerClient.copyArchiveFromContainerCmd(containerId, "/opt/app/errorReport.txt");
        try {
            logEntries.addAll(IOUtils.readLines(errorReport.exec(), Charset.forName(StandardCharsets.UTF_8.name())));
        } catch (NotFoundException e) {
            logEntries.add("errorReport.txt does not exists");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(ACHILLES_RESULTS_AVAILABLE_LOG, createAchillesOutputResults(dataSource, workDir));
        }
    }

    private Integer executeContainer(DockerClient dockerClient, String containerId, List<String> logEntries) {
        dockerClient.startContainerCmd(containerId).exec();
        LOGGER.debug("Container running: {}", containerId);
        WaitContainerResultCallback callback = dockerClient.waitContainerCmd(containerId)
                .exec(new WaitContainerResultCallback());

        ResultCallback.Adapter adapter = new ResultCallback.Adapter() {
            @Override
            public void onNext(Object item) {
                logEntries.add(item.toString());
                LOGGER.debug("{}", item);
            }
        };

        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .withTailAll()
                .exec(adapter);
        return callback.awaitStatusCode();
    }

    private CreateContainerResponse buildContainer(DockerClient dockerClient, DataSource dataSource, Path workDir, String imageName) {
        LOGGER.debug("Creating container: {}", imageName);
        Volume outputVolume = new Volume("/opt/app/output");
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(getTempLocationOnHost(workDir), outputVolume))
                .withNetworkMode(properties.getNetworkMode());

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withEnv(
                        env(ACHILLES_SOURCE, dataSource.getName()),
                        env(ACHILLES_DB_URI, dburi(dataSource)),
                        env(ACHILLES_CDM_SCHEMA, dataSource.getCdmSchema()),
                        env(ACHILLES_VOCAB_SCHEMA, dataSource.getCdmSchema()),
                        env(ACHILLES_RES_SCHEMA, getResultSchema(dataSource)),
                        env(ACHILLES_CDM_VERSION, Constants.Achilles.DEFAULT_CDM_VERSION),
                        env(ACHILLES_NUM_THREADS, valueOf(1))
                )
                .withVolumes(outputVolume)
                .withHostConfig(hostConfig)
                .exec();
        LOGGER.debug("Container created: {}", container.getId());
        return container;
    }

    private void pullAchillesImage(DockerClient dockerClient, String imageName) throws InterruptedException {
        LOGGER.debug("Pulling image: {}", imageName);
        AuthConfig authConfig = new AuthConfig()
                .withRegistryAddress(properties.getAuthConfig().getRegistryAddress())
                .withUsername(properties.getAuthConfig().getUsername())
                .withPassword(properties.getAuthConfig().getPassword());
        dockerClient.pullImageCmd(imageName)
                .withAuthConfig(authConfig)
                .exec(new PullImageResultCallback())
                .awaitCompletion(5, TimeUnit.MINUTES);
    }

    private Path createAchillesOutputResults(DataSource dataSource, Path workDir) throws IOException {

        LOGGER.debug("Creating Achilles output results archive");
        Path dataDir = workDir.resolve(dataSource.getName());
        Path jsonDir = null;
        jsonDir = dataDir;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    jsonDir = path;
                }
            }
        }
        return jsonDir;
    }

    private String dburi(DataSource dataSource) {

        String connStr = dataSource.getConnectionString().replace("jdbc:", "");
        int index = connStr.indexOf("://");
        String creds = dataSource.getUsername() + ":" + dataSource.getPassword() + "@";
        return new StringBuffer(connStr).insert(index + 3, creds).toString();
    }

    private String env(String name, String value) {

        return String.format("%s=%s", name, value);
    }
}
