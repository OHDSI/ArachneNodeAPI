/**
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
 * Created: July 04, 2017
 *
 */

package com.odysseusinc.arachne.datanode.scheduler;

import com.odysseusinc.arachne.datanode.service.AchillesService;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AchillesScheduler {

    private final AchillesService achillesService;
    private final DataSourceService dataSourceService;
    private final TaskScheduler scheduler;

    @Value("${achilles.scheduler.enabled}")
    private Boolean enabled;
    @Value("${achilles.scheduler.cron}")
    private String cron;

    @Autowired
    public AchillesScheduler(AchillesService achillesService,
                             DataSourceService dataSourceService,
                             TaskScheduler scheduler) {

        this.achillesService = achillesService;
        this.dataSourceService = dataSourceService;
        this.scheduler = scheduler;
    }

    @PostConstruct
    private void checkProperties() {

        if (enabled != null && enabled) {
            scheduler.schedule(new AchillesTask(achillesService, dataSourceService), new CronTrigger(cron));
        }
    }

    private static class AchillesTask implements Runnable {

        private final AchillesService achillesService;
        private final DataSourceService dataSourceService;

        private AchillesTask(AchillesService achillesService, DataSourceService dataSourceService) {

            this.achillesService = achillesService;
            this.dataSourceService = dataSourceService;
        }

        @Override
        public void run() {

            dataSourceService.findAll().forEach(achillesService::executeAchilles);
        }
    }
}
