package com.odysseusinc.arachne.datanode.controller.analysis;

import com.odysseusinc.arachne.datanode.dto.analysis.AnalysisFileDTO;
import com.odysseusinc.arachne.datanode.service.AnalysisResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/analysis/{parentId}/results")
public class AnalysisResultsController {
    @Autowired
    private AnalysisResultsService analysisResultsService;

    @GetMapping("/list")
    public List<AnalysisFileDTO> listResultFiles(@PathVariable("parentId") Long parentId) {
        return analysisResultsService.getAnalysisResults(parentId);
    }

    @GetMapping("/list/{filename}")
    public ResponseEntity<Resource> getResultFile(
            @PathVariable("parentId") Long parentId,
            @PathVariable("filename") String filename
    ) throws IOException {
        Resource resource = analysisResultsService.getAnalysisResultFile(parentId, filename);
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(Paths.get(resource.toString())))
                .body(resource);
    }

}
