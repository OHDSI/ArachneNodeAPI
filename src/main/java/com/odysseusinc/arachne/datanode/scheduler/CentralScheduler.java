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

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.DataSourceService;
import com.odysseusinc.arachne.datanode.service.UserRegistrationStrategy;
import com.odysseusinc.arachne.datanode.service.UserService;
import java.text.MessageFormat;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CentralScheduler {

    private static final Logger log = LoggerFactory.getLogger(CentralScheduler.class);
    private static final int LINE_WIDTH = 80;
    private static final String MODE_ERROR_MESSAGE = "Cannot switch mode from Standalone to Network - there are some {0} not linked to the Central.";

    private final DataNodeService dataNodeService;
    private final UserService userService;
    private final DataSourceService dataSourceService;
    @Value("${authenticator.user.registrationStrategy}")
    private String userRegistrationStrategy;

    public CentralScheduler(DataNodeService dataNodeService,
                            UserService userService,
                            DataSourceService dataSourceService) {

        this.dataNodeService = dataNodeService;
        this.userService = userService;
        this.dataSourceService = dataSourceService;
    }

    @PostConstruct
    public void checkRunMode() {

        switch (dataNodeService.getDataNodeMode()) {
            case STANDALONE:
                warnUserRegistration(); break;
            case NETWORK:
                checkModeSwitching(); break;
        }
    }

    private void checkModeSwitching() {

        List<User> users = userService.findStandaloneUsers();
        if (users.size() > 0) {
            throw new BeanInitializationException(MessageFormat.format(MODE_ERROR_MESSAGE, "users"));
        }
        List<DataSource> dataSources = dataSourceService.findStandaloneSources();
        if (dataSources.size() > 0) {
            throw new BeanInitializationException(MessageFormat.format(MODE_ERROR_MESSAGE, "data sources"));
        }
    }

    private void warnUserRegistration() {

        if (UserRegistrationStrategy.CREATE_IF_FIRST.equals(userRegistrationStrategy)) {
            StringBuilder sb = new StringBuilder("\n");
            sb.append(StringUtils.repeat("*", LINE_WIDTH)).append("\n");
            sb.append("\t\t").append("Running on the ").append(colorize("STAND-ALONE")).append(" mode").append("\n");
            sb.append("\t\t").append("UserRegistrationStrategy is set to ").append(colorize(userRegistrationStrategy)).append("\n");
            sb.append("\t\t").append("Please, ensure you are not using external Authenticator").append("\n");
            sb.append(StringUtils.repeat("*", LINE_WIDTH)).append("\n");
            log.warn(sb.toString());
        }
    }

    private String colorize(final String val) {

        return "\u001b[0;31m" + val + "\u001b[m";
    }

}
