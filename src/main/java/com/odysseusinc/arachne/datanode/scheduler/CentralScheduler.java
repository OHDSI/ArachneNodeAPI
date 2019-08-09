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
import com.odysseusinc.arachne.datanode.service.UserRegistrationStrategy;
import com.odysseusinc.arachne.datanode.service.UserService;
import com.odysseusinc.arachne.datanode.service.client.portal.CentralSystemClient;
import com.odysseusinc.arachne.datanode.service.events.FunctionalModeChangedEvent;
import com.odysseusinc.arachne.datanode.service.postpone.PostponeService;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CentralScheduler implements ApplicationListener<FunctionalModeChangedEvent> {

    private static final Logger log = LoggerFactory.getLogger(CentralScheduler.class);
    private static final int LINE_WIDTH = 80;

    private final CentralSystemClient systemClient;
    private final DataNodeService dataNodeService;
    private final UserService userService;
    private final PostponeService postponeService;
    @Value("${authenticator.user.registrationStrategy}")
    private String userRegistrationStrategy;

    public CentralScheduler(CentralSystemClient systemClient,
                            DataNodeService dataNodeService,
                            UserService userService,
                            PostponeService postponeService) {

        this.systemClient = systemClient;
        this.dataNodeService = dataNodeService;
        this.userService = userService;
        this.postponeService = postponeService;
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
            FunctionalMode oldMode = dataNodeService.getDataNodeMode();
            if (log.isDebugEnabled()) {
                log.debug("Central check failed: {}", e.getMessage());
            }
            dataNodeService.setDataNodeMode(FunctionalMode.STANDALONE);
            if (!Objects.equals(oldMode, FunctionalMode.STANDALONE)) {
                warnUserRegistration();
            }
        }
    }

    private void warnUserRegistration() {

        if (UserRegistrationStrategy.CREATE_IF_FIRST.equals(userRegistrationStrategy)) {
            StringBuilder sb = new StringBuilder("\n");
            sb.append(StringUtils.repeat("*", LINE_WIDTH)).append("\n");
            sb.append("\t\t").append("UserRegistrationStrategy is set to ").append(colorize(userRegistrationStrategy)).append("\n");
            sb.append("\t\t").append("Please, ensure you are not using external Authenticator").append("\n");
            sb.append(StringUtils.repeat("*", LINE_WIDTH)).append("\n");
            log.warn(sb.toString());
        }
    }

    private String colorize(final String val) {

        return "\u001b[0;31m" + val + "\u001b[m";
    }

    @Scheduled(fixedRateString = "${postponed.retry.interval}")
    public void retryFailedPostponedRequests() {

        if (Objects.equals(dataNodeService.getDataNodeMode(), FunctionalMode.NETWORK)) {
            postponeService.retryFailedRequests();
        }
    }

    @Override
    public void onApplicationEvent(FunctionalModeChangedEvent event) {

        if (Objects.equals(event.getMode(), FunctionalMode.NETWORK)) {
            postponeService.executePostponedRequests();
        }
    }
}
