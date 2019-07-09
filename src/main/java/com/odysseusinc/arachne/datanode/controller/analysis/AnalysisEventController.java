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

import com.odysseusinc.arachne.datanode.event.AnalysisResultEvent;
import com.odysseusinc.arachne.datanode.event.AnalysisUpdateEvent;
import com.odysseusinc.arachne.datanode.model.analysis.Analysis;
import com.odysseusinc.arachne.datanode.service.AnalysisService;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisExecutionStatusDTO;
import java.io.File;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.GenericConversionService;

public class AnalysisEventController {

	protected final GenericConversionService conversionService;
	protected final AnalysisService analysisService;

	public AnalysisEventController(GenericConversionService conversionService, AnalysisService analysisService) {

		this.conversionService = conversionService;
		this.analysisService = analysisService;
	}

	@EventListener
	public void proceed(AnalysisResultEvent resultEvent) {

		handleEvent(resultEvent);
	}

	@EventListener
	public void proceed(AnalysisUpdateEvent analysisUpdateEvent) {

		handleEvent(analysisUpdateEvent);
	}

	protected Analysis handleEvent(AnalysisResultEvent resultEvent) {

		Analysis analysis = conversionService.convert(resultEvent, Analysis.class);
		File resultDir = new File(analysis.getAnalysisFolder());

		return analysisService.saveResults(analysis, resultDir);
	}

	protected Optional<Analysis> handleEvent(AnalysisUpdateEvent updateEvent) {

		AnalysisExecutionStatusDTO status = updateEvent.getAnalysisExecutionStatus();
		Long id = status.getId();
		String stdout = status.getStdout();
		String password = updateEvent.getPassword();
		return analysisService.updateStatus(id, stdout, password);
	}
}
