package com.odysseusinc.arachne.datanode.service.preprocessors;

import com.odysseusinc.arachne.commons.annotations.PreprocessorComponent;
import com.odysseusinc.arachne.commons.service.preprocessor.Preprocessor;
import com.odysseusinc.arachne.commons.utils.CommonFileUtils;
import com.odysseusinc.arachne.datanode.dto.atlas.CohortDefinition;
import com.odysseusinc.arachne.datanode.service.CohortService;
import com.odysseusinc.arachne.datanode.service.SqlRenderService;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;

@PreprocessorComponent(contentType = CommonFileUtils.TYPE_COHORT_JSON)
public class CohortDefinitionPreprocessor implements Preprocessor<Analysis> {

    private final CohortService cohortService;
    private final CohortExpressionQueryBuilder queryBuilder = new CohortExpressionQueryBuilder();
    private final CohortPreprocessor cohortPreprocessor;
    private final SqlRenderService sqlRenderService;

    @Autowired
    public CohortDefinitionPreprocessor(CohortService cohortService,
                                        CohortPreprocessor cohortPreprocessor,
                                        SqlRenderService sqlRenderService) {

        this.cohortService = cohortService;
        this.cohortPreprocessor = cohortPreprocessor;
        this.sqlRenderService = sqlRenderService;
    }

    @Override
    public void preprocess(Analysis analysis, File file) {

        try {
            CohortDefinition definition = new CohortDefinition();
            definition.setExpression(FileUtils.readFileToString(file, "UTF-8"));
            String expressionSql = sqlRenderService.renderSql(definition);
            File sqlFile = new File(analysis.getAnalysisFolder(), analysis.getTitle() + ".sql");
            FileUtils.write(sqlFile, expressionSql, Charset.forName("UTF-8"));
            cohortPreprocessor.preprocess(analysis, sqlFile);
            analysis.setExecutableFileName(sqlFile.getName());
        } catch (IOException e) {
            throw new RuntimeIOException(e.getMessage(), e);
        }
    }
}
