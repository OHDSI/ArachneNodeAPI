package com.odysseusinc.arachne.datanode.service;

import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFile;

import java.io.File;
import java.util.List;

public interface AnalysisResultsService {

    List<AnalysisFile> getAnalysisResults(Analysis analysis);

    Analysis saveResults(Analysis analysis, File resultDir);
}
