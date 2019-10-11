package com.odysseusinc.arachne.datanode.service.preprocessors;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.odysseusinc.arachne.commons.annotations.PreprocessorComponent;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.service.preprocessor.Preprocessor;
import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.commons.utils.CommonFileUtils;
import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.service.CohortService;
import com.odysseusinc.arachne.datanode.service.impl.CohortServiceImpl;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.ohdsi.sql.SqlTranslate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;


@PreprocessorComponent(contentType = CommonFileUtils.TYPE_COHORT_SQL)
public class CohortPreprocessor implements Preprocessor<Analysis> {

    private static final String TARGET_COHORT_TABLE = "cohort";

    private final CohortService cohortService;
    @Value("${cohorts.result.countEnabled}")
    private Boolean countEnabled;
    @Value("${cohorts.result.summaryEnabled}")
    private Boolean summaryEnabled;

    @Autowired
    public CohortPreprocessor(CohortService cohortService) {

        this.cohortService = cohortService;
    }

    @Override
    public void preprocess(Analysis analysis, File file) {

        final String executableFileName = analysis.getExecutableFileName();
        final String name = file.getName();
        final boolean executableFile = executableFileName.equals(name);

        if (!cohortService.isPreprocessingIgnored(file) && (CommonAnalysisType.COHORT == analysis.getType() || executableFile)) {
            final DataSource dataSource = analysis.getDataSource();
            final int targetCohortId = new Random().nextInt(Integer.MAX_VALUE - 1) + 1;
            final String cdmSchema = dataSource.getCdmSchema();
            final DBMSType target = dataSource.getType();

            String targetDbSchema = !StringUtils.isEmpty(dataSource.getTargetSchema()) ? dataSource.getTargetSchema() : cdmSchema;
            String resultDbSchema = !StringUtils.isEmpty(dataSource.getResultSchema()) ? dataSource.getResultSchema() : cdmSchema;
            String targetCohortTable =
                    !StringUtils.isEmpty(dataSource.getCohortTargetTable()) ? dataSource.getCohortTargetTable() : TARGET_COHORT_TABLE;
            final CohortServiceImpl.TranslateOptions options = new CohortServiceImpl.TranslateOptions(
                    cdmSchema, targetDbSchema, resultDbSchema, cdmSchema, targetCohortTable, targetCohortId);

            String fileSuffix = target.getOhdsiDB();
            final ImmutableMap.Builder<String, ClassPathResource> mapBuilder = ImmutableMap.builder();
            if (analysis.getType() == CommonAnalysisType.COHORT) {
                fileSuffix = "cohort-" + fileSuffix;
                if (countEnabled) {
                    mapBuilder.put("count", new ClassPathResource("cohort/cohort-count.sql"));
                }
                if (summaryEnabled) {
                    mapBuilder.put("summary", new ClassPathResource("cohort/cohort-summary.sql"));
                }
            }
            final ImmutableMap<String, ClassPathResource> satellites = mapBuilder.build();
            try {
                final String sessionId = SqlTranslate.generateSessionId();
                final String sourceStatement = FileUtils.readFileToString(file, Charset.defaultCharset());
                final String nativeStatement
                        = cohortService.translateSQL(sourceStatement, null, target, sessionId, resultDbSchema, options);
                final String originalPath = file.getAbsolutePath();
                final String destinationPath = addSuffixToSqlFile(originalPath, fileSuffix);
                final File destinationFile = new File(destinationPath);
                Files.write(nativeStatement, destinationFile, Charset.defaultCharset());
                if (executableFile) {
                    analysis.setExecutableFileName(destinationFile.getName());
                }
                FileUtils.deleteQuietly(file);
                satellites.forEach((key, resource) -> {
                    try {
                        final String satelliteFileName
                                = addSuffixToSqlFile(originalPath, key + "-" + target.getOhdsiDB());
                        final String satelliteSourceStatement
                                = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());
                        final String satelliteNativeStatement
                                = cohortService.translateSQL(satelliteSourceStatement, null, target, sessionId, resultDbSchema,  options);
                        FileUtils.writeByteArrayToFile(new File(satelliteFileName), satelliteNativeStatement.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeIOException(e.getMessage(), e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeIOException(e.getMessage(), e);
            }
        }
    }

    private String addSuffixToSqlFile(String filename, String suffix) {

        String fileNameWoExt = filename.substring(0, filename.toLowerCase().lastIndexOf(".sql"));
        return String.format("%s-%s.sql", fileNameWoExt, suffix);
    }

}