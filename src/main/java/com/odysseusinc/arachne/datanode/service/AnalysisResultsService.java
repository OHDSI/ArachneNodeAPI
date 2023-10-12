package com.odysseusinc.arachne.datanode.service;

import com.odysseusinc.arachne.datanode.dto.analysis.AnalysisFileDTO;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFile;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.List;

public interface AnalysisResultsService {

    List<AnalysisFile> getAnalysisResults(Analysis analysis);

    List<AnalysisFileDTO> getAnalysisResults(Long analysisId);

    Resource getAnalysisResultFile(Long analysisId, String filename);

    Analysis saveResults(Analysis analysis, File resultDir);
}
