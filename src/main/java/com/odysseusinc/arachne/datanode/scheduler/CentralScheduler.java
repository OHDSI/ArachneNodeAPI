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
 * Created: Jul 25, 2019
 *
 */

package com.odysseusinc.arachne.datanode.scheduler;

import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.UserService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.events.FunctionalModeChangedEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CentralScheduler implements ApplicationListener<FunctionalModeChangedEvent> {

    private static final Logger log = LoggerFactory.getLogger(CentralScheduler.class);

    private final CentralSystemClient systemClient;
    private final DataNodeService dataNodeService;
    private final UserService userService;

    public CentralScheduler(CentralSystemClient systemClient, DataNodeService dataNodeService, UserService userService) {

        this.systemClient = systemClient;
        this.dataNodeService = dataNodeService;
        this.userService = userService;
    }

    @Scheduled(fixedRateString = "${central.scheduler.checkingInterval}")
    public void checkCentralAccessibility() {

        try {
            if (log.isDebugEnabled()) {
                log.debug("Checking Central availability");
            }
            systemClient.getBuildNumber();
            dataNodeService.setDataNodeMode(FunctionalMode.NETWORK);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Central check failed: {}", e.getMessage());
            }
            dataNodeService.setDataNodeMode(FunctionalMode.STANDALONE);
        }
    }

    @Override
    public void onApplicationEvent(FunctionalModeChangedEvent event) {

        if (Objects.equals(event.getMode(), FunctionalMode.NETWORK)) {
            userService.syncUsers();
        }
    }
}
