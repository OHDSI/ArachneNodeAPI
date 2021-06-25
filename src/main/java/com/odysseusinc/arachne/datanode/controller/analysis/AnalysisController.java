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

package com.odysseusinc.arachne.datanode.controller.analysis;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonAnalysisType;
import com.odysseusinc.arachne.commons.api.v1.dto.OptionDTO;
import com.odysseusinc.arachne.commons.utils.ZipUtils;
import com.odysseusinc.arachne.datanode.dto.analysis.AnalysisRequestDTO;
import com.odysseusinc.arachne.datanode.exception.IllegalOperationException;
import com.odysseusinc.arachne.datanode.exception.NotExistException;
import com.odysseusinc.arachne.datanode.exception.PermissionDeniedException;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisAuthor;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisFile;
import com.odysseusinc.arachne.datanode.model.analysis.AnalysisOrigin;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.AnalysisResultsService;
import com.odysseusinc.arachne.datanode.service.AnalysisService;
import com.odysseusinc.arachne.datanode.service.UserService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);
    private static final String ERROR_MESSAGE = "Failed to save analysis files";
    private final AnalysisService analysisService;
    private final AnalysisResultsService analysisResultsService;
    private final UserService userService;

    private final GenericConversionService conversionService;

	public AnalysisController(AnalysisService analysisService,
                              AnalysisResultsService analysisResultsService,
                              UserService userService,
                              GenericConversionService conversionService) {

		this.analysisService = analysisService;
		this.analysisResultsService = analysisResultsService;
        this.userService = userService;
        this.conversionService = conversionService;
    }

	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity executeAnalysis(
            @RequestPart("file") List<MultipartFile> archive,
            @RequestPart("analysis") @Valid AnalysisRequestDTO analysisRequestDTO,
            Principal principal
            ) throws PermissionDeniedException {

	    try {
            Analysis analysis = conversionService.convert(analysisRequestDTO, Analysis.class);

            analysis.setOrigin(AnalysisOrigin.DIRECT_UPLOAD);
            User user = userService.getUser(principal);
            if (Objects.nonNull(user)) {
                AnalysisAuthor author = conversionService.convert(user, AnalysisAuthor.class);
                analysis.setAuthor(author);
            }
            analysisService.saveAnalysisFiles(analysis, archive);
            analysisService.persist(analysis);
            analysisService.sendToEngine(analysis);

            return ResponseEntity.ok().build();
        } catch (IOException e) {
	        logger.error(ERROR_MESSAGE, e);
	        throw new IllegalOperationException(ERROR_MESSAGE);
        }
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "{id}/results",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public void downloadResults(@PathVariable("id") Long analysisId, HttpServletResponse response) throws IOException {

	    Analysis analysis = analysisService.findAnalysis(analysisId)
                .orElseThrow(() -> new NotExistException(Analysis.class));
	    List<AnalysisFile> resultFiles = analysisResultsService.getAnalysisResults(analysis);
	    Path stdoutDir = Files.createTempDirectory("node_analysis");
	    Path stdoutFile = stdoutDir.resolve("stdout.txt");
	    try(Writer writer = new FileWriter(stdoutFile.toFile())) {
	        IOUtils.write(analysis.getStdout(), writer);
        }

        // mergeSplitFiles doesn't work with existing file, so cannot do Files.createTempFile()
        final File archive = new File(System.getProperty("java.io.tmpdir"), "results" + UUID.randomUUID() + ".zip");

	    // find and merge split archive main file
	    for (final AnalysisFile analysisFile: resultFiles) {
	        try {
                final ZipFile file = new ZipFile(analysisFile.getLink());
                if (file.isSplitArchive()) {
                    file.mergeSplitFiles(archive);
                } else if (file.isValidZipFile()) { // in case of single archive file
                    Files.copy(Paths.get(analysisFile.getLink()), Paths.get(archive.toURI()));
                }
            } catch (final ZipException ze) {
	            //ignore: isSplitArchive() throws this, if the file is not zip
            }
        }

	    // add stdout to archive
        new ZipFile(archive).addFiles(Collections.singletonList(stdoutFile.toFile()));

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-disposition", "attachment; filename=" + archive.getName());
        try(InputStream in = new FileInputStream(archive)) {
            IOUtils.copy(in, response.getOutputStream());
        } finally {
            FileUtils.deleteQuietly(archive);
            FileUtils.deleteQuietly(stdoutDir.toFile());
        }
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "/types",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public List<OptionDTO> getTypes() {

	    return Stream.of(CommonAnalysisType.values())
                .map(type -> new OptionDTO(type.name(), type.getTitle()))
                .collect(Collectors.toList());
    }

}
