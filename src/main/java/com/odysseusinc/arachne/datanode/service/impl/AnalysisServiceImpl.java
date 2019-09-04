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

import com.odysseusinc.arachne.datanode.repository.AnalysisFileRepository;
import com.odysseusinc.arachne.datanode.repository.AnalysisRepository;
import com.odysseusinc.arachne.datanode.repository.AnalysisStateJournalRepository;
import com.odysseusinc.arachne.datanode.service.ExecutionEngineIntegrationService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;

@Service
public class AnalysisServiceImpl extends BaseAnalysisServiceImpl {

	public AnalysisServiceImpl(GenericConversionService conversionService,
														 AnalysisPreprocessorService preprocessorService,
														 AnalysisRepository analysisRepository,
														 AnalysisStateJournalRepository analysisStateJournalRepository,
														 AnalysisFileRepository analysisFileRepository,
														 ExecutionEngineIntegrationService engineIntegrationService) {

		super(conversionService,
                preprocessorService,
				analysisRepository,
				analysisStateJournalRepository,
				analysisFileRepository,
				engineIntegrationService);
	}
}
