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
 * Created: June 27, 2017
 *
 */

package com.odysseusinc.arachne.datanode.scheduler;

import com.odysseusinc.arachne.commons.api.v1.dto.AtlasInfoDTO;
import com.odysseusinc.arachne.datanode.service.CohortService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import feign.RetryableException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CohortScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CohortScheduler.class);

    private static final String ATLAS_VERSION_CHECKING_LOG = "Checking version of installed atlas";
    private static final String ATLAS_VERSION_PASSED_LOG
            = "Atlas version='{}' allow to build cohort, adding scheduled task";
    private static final String ATLAS_VERSION_NOT_PASSED_LOG
            = "Atlas version='{}' does not allow to build cohort";
    private static final String ATLAS_NOT_INSTALLED_LOG
            = "Atlas is not installed on specified host/port. Error: {}";

    private final DataNodeService dataNodeService;
    private final AtlasClient atlasClient;
    private final CentralSystemClient centralClient;
    private final TaskScheduler scheduler;
    private final CohortService cohortService;

    @Value("${entities.scheduler.checkListRequestsInterval}")
    private Long listRequestInterval;
    @Value("${entities.scheduler.checkRequestInterval}")
    private Long requestInterval;

    @Autowired
    public CohortScheduler(DataNodeService dataNodeService,
                           AtlasClient atlasClient,
                           CentralSystemClient centralClient,
                           TaskScheduler scheduler,
                           CohortService cohortService) {

        this.dataNodeService = dataNodeService;
        this.atlasClient = atlasClient;
        this.centralClient = centralClient;
        this.scheduler = scheduler;
        this.cohortService = cohortService;
    }

    private final Set<ScheduledFuture> cohortTask = new HashSet<>();

    @Scheduled(fixedRateString = "${atlas.scheduler.checkInterval}")
    public void checkAtlas() {

        dataNodeService.findCurrentDataNode().ifPresent(dataNode -> {
            Boolean atlasInsalled = false;
            String atlasVersion = null;
            LOGGER.debug(ATLAS_VERSION_CHECKING_LOG);
            try {
                final AtlasClient.Info info = atlasClient.getInfo();
                atlasVersion = info.version;
            } catch (RetryableException e) {
                LOGGER.debug(ATLAS_NOT_INSTALLED_LOG, e.getMessage());
            }
            if (atlasVersion != null && atlasVersion.matches(AtlasInfoDTO.ATLAS_VERSION_REGEX)) {
                LOGGER.debug(ATLAS_VERSION_PASSED_LOG, atlasVersion);
                atlasInsalled = true;
                if (cohortTask.isEmpty()) {
                    final CohortListRequestTask cohortListRequestTask = new CohortListRequestTask(cohortService);
                    final CohortRequestTask cohortRequestTask = new CohortRequestTask(cohortService);
                    cohortTask.add(scheduler.scheduleWithFixedDelay(cohortListRequestTask, listRequestInterval));
                    cohortTask.add(scheduler.scheduleWithFixedDelay(cohortRequestTask, requestInterval));
                }
            } else {
                LOGGER.debug(ATLAS_VERSION_NOT_PASSED_LOG, atlasVersion);
                if (!cohortTask.isEmpty()) {
                    final Iterator<ScheduledFuture> iterator = cohortTask.iterator();
                    while (iterator.hasNext()) {
                        final ScheduledFuture scheduledFuture = iterator.next();
                        scheduledFuture.cancel(false);
                        iterator.remove();
                    }
                }
            }
            centralClient.sendAtlasInformation(new AtlasInfoDTO(atlasInsalled, atlasVersion));
        });
    }

    private class CohortListRequestTask implements Runnable {

        private final CohortService cohortService;

        private CohortListRequestTask(CohortService cohortService) {

            this.cohortService = cohortService;
        }

        @Override
        public void run() {

            cohortService.checkListRequests();
        }
    }

    private final class CohortRequestTask extends CohortListRequestTask {

        private CohortRequestTask(CohortService cohortService) {

            super(cohortService);
        }

        @Override
        public void run() {

            cohortService.checkCohortRequest();
        }
    }
}
