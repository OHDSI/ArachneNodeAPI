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
 * Created: May 18, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.impl;

import static com.odysseusinc.arachne.datanode.Constants.Achilles.ACHILLES_CDM_SCHEMA;
import static com.odysseusinc.arachne.datanode.Constants.Achilles.ACHILLES_CDM_VERSION;
import static com.odysseusinc.arachne.datanode.Constants.Achilles.ACHILLES_DB_URI;
import static com.odysseusinc.arachne.datanode.Constants.Achilles.ACHILLES_RES_SCHEMA;
import static com.odysseusinc.arachne.datanode.Constants.Achilles.ACHILLES_SOURCE;
import static com.odysseusinc.arachne.datanode.Constants.Achilles.ACHILLES_VOCAB_SCHEMA;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.Achilles.LIST_REPORTS;
import static com.odysseusinc.arachne.datanode.Constants.CentralApi.Achilles.PARAM_DATANODE;
import static com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus.FAILED;
import static com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus.IN_PROGRESS;
import static com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus.SUCCESSFUL;
import static com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors.resultSet;
import static com.odysseusinc.arachne.datanode.util.datasource.QueryProcessors.statement;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAchillesReportDTO;
import com.odysseusinc.arachne.datanode.Constants;
import com.odysseusinc.arachne.datanode.config.properties.AchillesProperties;
import com.odysseusinc.arachne.datanode.exception.AchillesJobInProgressException;
import com.odysseusinc.arachne.datanode.exception.ArachneSystemRuntimeException;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJob;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJobSource;
import com.odysseusinc.arachne.datanode.model.achilles.AchillesJobStatus;
import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.repository.AchillesJobRepository;
import com.odysseusinc.arachne.datanode.service.AchillesService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.achilles.AchillesProcessors;
import com.odysseusinc.arachne.datanode.service.achilles.ConditionEraReport;
import com.odysseusinc.arachne.datanode.service.achilles.ConditionReport;
import com.odysseusinc.arachne.datanode.service.achilles.DashboardReport;
import com.odysseusinc.arachne.datanode.service.achilles.DataDensityReport;
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
import com.odysseusinc.arachne.datanode.util.ZipUtils;
import com.odysseusinc.arachne.datanode.util.datasource.ResultSetProcessor;
import com.odysseusinc.arachne.datanode.util.datasource.ResultTransformers;
import com.odysseusinc.arachne.datanode.util.datasource.ResultWriters;
import com.odysseusinc.arachne.execution_engine_common.util.CommonFileUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class AchillesServiceImpl implements AchillesService {

    public static final String ACHILLES_HEEL_SQL = "classpath:/achilles/data/export_v5/achillesheel/sqlAchillesHeel.sql";
    public static final String DRUG_ERA_SQL = "classpath:/achilles/data/export_v5/drugera/sqlDrugEraTreemap.sql";
    public static final String DRUG_TREEMAP_SQL = "classpath:/achilles/data/export_v5/drug/sqlDrugTreemap.sql";
    public static final String CONDITION_TREEMAP_SQL = "classpath:/achilles/data/export_v5/condition/sqlConditionTreemap.sql";
    public static final String CONDITION_ERA_TREEMAP_SQL = "classpath:/achilles/data/export_v5/conditionera/sqlConditionEraTreemap.sql";
    public static final String PROCEDURE_TREEMAP_SQL = "classpath:/achilles/data/export_v5/procedure/sqlProcedureTreemap.sql";
    public static final String MEASUREMENT_TREEMAP_SQL = "classpath:/achilles/data/export_v5/measurement/sqlMeasurementTreemap.sql";
    public static final String OBSERVATION_TREEMAP_SQL = "classpath:/achilles/data/export_v5/observation/sqlObservationTreemap.sql";
    protected static final Logger LOGGER = LoggerFactory.getLogger(AchillesService.class);
    protected static final String DATA_NODE_NOT_EXISTS_EXCEPTION = "DataNode is not registered, please register";
    protected final DockerClient dockerClient;
    protected final AchillesProperties properties;
    protected final RestTemplate restTemplate;
    protected final CentralUtil centralUtil;
    protected final RetryTemplate retryTemplate;
    protected final DataNodeService dataNodeService;
    protected final AchillesJobRepository achillesJobRepository;
    @Value("${datanode.arachneCentral.host}")
    protected String centralHost;
    @Value("${datanode.arachneCentral.port}")
    protected Integer centralPort;

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
    public AchillesServiceImpl(DockerClient dockerClient,
                               AchillesProperties properties,
                               @Qualifier("executionEngineRestTemplate") RestTemplate restTemplate,
                               CentralUtil centralUtil,
                               @Qualifier("achillesRetryTemplate") RetryTemplate retryTemplate,
                               DataNodeService dataNodeService,
                               AchillesJobRepository achillesJobRepository) {

        this.dockerClient = dockerClient;
        this.properties = properties;
        this.restTemplate = restTemplate;
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
        achillesJobRepository.save(jobs);
    }

    @Async
    @Override
    public void executeAchilles(DataSource dataSource) {

        LOGGER.info("Starting achilles for: {}", dataSource);
        AchillesJob job = checkAchillesJob(dataSource, AchillesJobSource.GENERATION);
        try {
            Path results = runAchilles(dataSource, job);
            retryTemplate.execute((RetryCallback<Void, Exception>) retryContext -> {

                sendResultToCentral(dataSource, results);
                return null;
            });

            updateJob(job, SUCCESSFUL);
        } catch (Throwable e) {
            LOGGER.error("Achilles failed to execute", e);
            updateJob(job, FAILED);
        }
    }

    private AchillesJob checkAchillesJob(DataSource dataSource, AchillesJobSource source) {

        Optional<AchillesJob> achillesJob = achillesJobRepository
                .findTopByDataSourceAndStatusOrderByStarted(dataSource, IN_PROGRESS);
        achillesJob.ifPresent(job -> {
            LOGGER.warn("Achilles is in progress for datasource: {}", dataSource);
            throw new AchillesJobInProgressException();
        });
        return createJob(dataSource, source);
    }

    @Override
    public boolean hasAchillesResultTable(DataSource dataSource) {

        try {
            String query = "select count(*) from %s.achilles_results";
            query = String.format(query, getResultSchema(dataSource));
            Map<String, Integer> result = DataSourceUtils.<Integer>withDataSource(dataSource)
                    .run(statement(query))
                    .collectResults(resultSet -> {
                        Map<String, Integer> data = new HashMap<>();
                        int count = 0;
                        if (resultSet.next()){
                            count = resultSet.getInt(1);
                        }
                        data.put("count", count);
                        return data;
                    })
                    .getResults();
            return result.getOrDefault("count", 0) > 0;
        } catch (SQLException e) {
            LOGGER.warn("Failed to check achilles results", e);
            return false;
        }
    }

    @Async
    @Override
    public void pullAchillesData(DataSource dataSource) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Start pulling Achilles data for {}", dataSource);
        }
        AchillesJob job = checkAchillesJob(dataSource, AchillesJobSource.IMPORT);
        try {
            Path tempDir = Files.createTempDirectory("achilles_");
            ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setDaemon(false)
                    .setThreadFactory(Executors.defaultThreadFactory())
                    .setNameFormat("achilles-pool-%d")
                    .build();
            ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);
            try {
                List<Callable<String>> tasks = new LinkedList<>();
                tasks.add(achillesTask("Heel", () -> runAchillesQuery(dataSource, ACHILLES_HEEL_SQL, tempDir.resolve("achillesheel.json"),
                        AchillesProcessors.achillesHeel())));
                tasks.add(achillesTask("DrugEra", () -> {
                    List<Integer> drugEraConcepts = new LinkedList<>();
                    Integer result = runAchillesQuery(dataSource, DRUG_ERA_SQL, tempDir.resolve("drugera_treemap.json"),
                            resultSet(),
                            transmitToList("CONCEPT_ID", drugEraConcepts));
                    result += drugEraReport.runReports(dataSource, tempDir.resolve("drugeras"), drugEraConcepts);
                    return result;
                }));
                tasks.add(achillesTask("Drugs", () -> {
                    List<Integer> drugConcepts = new LinkedList<>();
                    Integer result = runAchillesQuery(dataSource, DRUG_TREEMAP_SQL, tempDir.resolve("drug_treemap.json"),
                            resultSet(),
                            transmitToList("CONCEPT_ID", drugConcepts));
                    result += drugReport.runReports(dataSource, tempDir.resolve("drugs"), drugConcepts);
                    return result;
                }));
                tasks.add(achillesTask("Conditions", () -> {
                    List<Integer> conditionConcepts = new LinkedList<>();
                    Integer result = runAchillesQuery(dataSource, CONDITION_TREEMAP_SQL, tempDir.resolve("condition_treemap.json"),
                            resultSet(),
                            transmitToList("CONCEPT_ID", conditionConcepts));
                    result += conditionReport.runReports(dataSource, tempDir.resolve("conditions"), conditionConcepts);
                    return result;
                }));
                tasks.add(achillesTask("ConditionEra", () -> {
                    List<Integer> conditionEraConcepts = new LinkedList<>();
                    Integer result = runAchillesQuery(dataSource, CONDITION_ERA_TREEMAP_SQL, tempDir.resolve("conditionera_treemap.json"),
                            resultSet(),
                            transmitToList("CONCEPT_ID", conditionEraConcepts));
                    result += conditionEraReport.runReports(dataSource, tempDir.resolve("conditioneras"), conditionEraConcepts);
                    return result;
                }));
                tasks.add(achillesTask("Procedure", () -> {
                    List<Integer> procedureConcepts = new LinkedList<>();
                    Integer result = runAchillesQuery(dataSource, PROCEDURE_TREEMAP_SQL, tempDir.resolve("procedure_treemap.json"),
                            resultSet(),
                            transmitToList("CONCEPT_ID", procedureConcepts));
                    result += procedureReport.runReports(dataSource, tempDir.resolve("procedures"), procedureConcepts);
                    return result;
                }));
                tasks.add(achillesTask("Person", () -> personReport.runReports(dataSource, tempDir.resolve("person.json"), null)));
                tasks.add(achillesTask("ObservationPeriod", () -> observationPeriodReport.runReports(dataSource, tempDir.resolve("observationperiod.json"), null)));
                tasks.add(achillesTask("Dashboard", () -> dashboardReport.runReports(dataSource, tempDir.resolve("dashboard.json"), null)));
                tasks.add(achillesTask("DataDensity", () -> dataDensityReport.runReports(dataSource, tempDir.resolve("datadensity.json"), null)));
                tasks.add(achillesTask("Measurement", () -> {
                    List<Integer> measurementConcepts = new LinkedList<>();
                    Integer result = runAchillesQuery(dataSource, MEASUREMENT_TREEMAP_SQL, tempDir.resolve("measurement_treemap.json"),
                            resultSet(),
                            transmitToList("CONCEPT_ID", measurementConcepts));
                    result += measurementReport.runReports(dataSource, tempDir.resolve("measurements"), measurementConcepts);
                    return result;
                }));
                tasks.add(achillesTask("Observation", () -> {
                    List<Integer> observationConcepts = new LinkedList<>();
                    Integer result = runAchillesQuery(dataSource, OBSERVATION_TREEMAP_SQL, tempDir.resolve("observation_treemap.json"),
                            resultSet(),
                            transmitToList("CONCEPT_ID", observationConcepts));
                    result += observationReport.runReports(dataSource, tempDir.resolve("observations"), observationConcepts);
                    return result;
                }));
                tasks.add(achillesTask("Visit", () -> {
                    List<Integer> visitConcepts = new LinkedList<>();
                    Integer result = runAchillesQuery(dataSource, "classpath:/achilles/data/export_v5/visit/sqlVisitTreemap.sql", tempDir.resolve("visit_treemap.json"),
                            resultSet(),
                            transmitToList("CONCEPT_ID", visitConcepts));
                    result += visitReport.runReports(dataSource, tempDir.resolve("visits"), visitConcepts);
                    return result;
                }));

                try {
                    List<Future<String>> futures = executorService.invokeAll(tasks);
                    for (Future<String> future : futures) {
                        LOGGER.info(future.get());
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
        } catch (Throwable e) {
            LOGGER.error("Failed to pull achilles results", e);
            updateJob(job, FAILED);
        }
    }

    private String getResultSchema(DataSource dataSource) {

        return StringUtils.defaultIfEmpty(dataSource.getResultSchema(), dataSource.getCdmSchema());
    }

    private Callable<String> achillesTask(String name, Callable<Integer> callable){

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

    private Integer runAchillesQuery(DataSource dataSource, String queryPath, Path targetPath)
            throws IOException, SQLException {

        return runAchillesQuery(dataSource, queryPath, targetPath, resultSet(), null);
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
    public List<CommonAchillesReportDTO> getAchillesReports() throws NotExistException {

        DataNode dataNode = dataNodeService.findCurrentDataNode()
                .orElseThrow(() -> new NotExistException(DATA_NODE_NOT_EXISTS_EXCEPTION,
                        DataNode.class));
        String token = dataNode.getToken();
        HttpHeaders headers = centralUtil.getCentralNodeAuthHeader(token);
        HttpEntity requestEntity = new HttpEntity(headers);
        URI uri = UriComponentsBuilder.fromHttpUrl(centralHost + ":" + centralPort
                + LIST_REPORTS)
                .queryParam(PARAM_DATANODE, dataNode.getCentralId())
                .build().encode().toUri();
        ResponseEntity<List<CommonAchillesReportDTO>> entity = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<CommonAchillesReportDTO>>() {
                });
        if (!HttpStatus.OK.equals(entity.getStatusCode())) {
            String message = String.format("Failed to get achilles reports, status code: %d",
                    entity.getStatusCode().value());
            LOGGER.error(message);
            throw new ArachneSystemRuntimeException(message);
        }
        return entity.getBody();
    }

    private void sendResultToCentral(DataSource dataSource, Path results) throws IOException, NotExistException {

        LOGGER.debug("Sending results to central: {}", centralHost);
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new FileSystemResource(results.toFile()));
        Optional<DataNode> dataNode = dataNodeService.findCurrentDataNode();
        String token = dataNode.orElseThrow(() -> new NotExistException(DATA_NODE_NOT_EXISTS_EXCEPTION,
                DataNode.class)).getToken();

        HttpHeaders headers = centralUtil.getCentralNodeAuthHeader(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(map, headers);


        File file = results.toFile();
        File targetFile = new File("/tmp", "archive"+ UUID.randomUUID());
        CommonFileUtils.compressAndSplit(file, targetFile, null);

        FileInputStream input = new FileInputStream(targetFile);
        MultipartFile multipartFile = new MockMultipartFile("file",
                file.getName(), "text/plain", IOUtils.toByteArray(input));

        centralSystemClient.sendAchillesResults(dataSource.getCentralId(), multipartFile);

        LOGGER.debug("Results successfully sent");

        FileUtils.deleteQuietly(results.toFile());
    }

    private Path runAchilles(final DataSource dataSource, AchillesJob job) throws IOException {

        Path workDir = Files.createTempDirectory("achilles_");
        LOGGER.debug("Achilles result directory: {}", workDir);
        try {
            String imageName = properties.getImageName();
            LOGGER.debug("Pulling image: {}", imageName);
            AuthConfig authConfig = new AuthConfig()
                    .withRegistryAddress(properties.getAuthConfig().getRegistryAddress())
                    .withUsername(properties.getAuthConfig().getUsername())
                    .withPassword(properties.getAuthConfig().getPassword());
            dockerClient.pullImageCmd(imageName)
                    .withAuthConfig(authConfig)
                    .exec(new PullImageResultCallback())
                    .awaitSuccess();
            LOGGER.debug("Creating container: {}", imageName);
            Volume outputVolume = new Volume("/opt/app/output");
            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withEnv(
                            env(ACHILLES_SOURCE, dataSource.getName()),
                            env(ACHILLES_DB_URI, dburi(dataSource)),
                            env(ACHILLES_CDM_SCHEMA, dataSource.getCdmSchema()),
                            env(ACHILLES_VOCAB_SCHEMA, dataSource.getCdmSchema()),
                            env(ACHILLES_RES_SCHEMA, getResultSchema(dataSource)),
                            env(ACHILLES_CDM_VERSION, Constants.Achilles.DEFAULT_CDM_VERSION)
                    )
                    .withVolumes(outputVolume)
                    .withBinds(new Bind(workDir.toString(), outputVolume))
                    .withNetworkMode("host")
                    .exec();
            LOGGER.debug("Container created: {}", container.getId());
            try {
                LOGGER.debug("Starting container: {}", container.getId());
                dockerClient.startContainerCmd(container.getId()).exec();
                LOGGER.debug("Container running: {}", container.getId());
                WaitContainerResultCallback callback = dockerClient.waitContainerCmd(container.getId())
                        .exec(new WaitContainerResultCallback());
                List<String> logEntries = new LinkedList<>();
                LogContainerResultCallback logCallback = new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame item) {

                        logEntries.add(item.toString());
                        LOGGER.debug(item.toString());
                    }
                };
                dockerClient.logContainerCmd(container.getId())
                        .withStdOut(true)
                        .withStdErr(true)
                        .withFollowStream(true)
                        .withTailAll()
                        .exec(logCallback);
                Integer statusCode = callback.awaitStatusCode();
                try {
                    logCallback.awaitCompletion(3, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                LOGGER.debug("Achilles finished with status code: {}", statusCode);
                job.setAchillesLog(logEntries.stream().collect(Collectors.joining("\n")));
                achillesJobRepository.save(job);
                if (statusCode == 0) {
                    LOGGER.debug("Creating Achilles output results archive");
                    Path archiveFile = Files.createTempFile("achilles_", ".zip");
                    Path dataDir = workDir.resolve(dataSource.getName());
                    Path jsonDir = dataDir;
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDir)) {
                        for (Path path : directoryStream) {
                            if (Files.isDirectory(path)) {
                                jsonDir = path;
                            }
                        }
                    }
                    ZipUtils.zipDirectory(archiveFile, jsonDir);
                    LOGGER.info("Achilles results available at: {}", archiveFile);
                    return archiveFile;
                } else {
                    String message = "Achilles exited with errors, lost results";
                    LOGGER.warn(message);
                    throw new IOException(message);
                }
            } finally {
                dockerClient.removeContainerCmd(container.getId())
                        .withRemoveVolumes(true)
                        .exec();
            }
        } finally {
            FileUtils.deleteQuietly(workDir.toFile());
        }
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
