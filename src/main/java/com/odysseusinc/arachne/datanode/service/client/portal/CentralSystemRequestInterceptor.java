/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
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
 * Created: August 28, 2017
 *
 */

package com.odysseusinc.arachne.datanode.service.client.portal;

import com.odysseusinc.arachne.datanode.model.datanode.DataNode;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import feign.RequestTemplate;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CentralSystemRequestInterceptor implements feign.RequestInterceptor {

    @Value("${datanode.arachneCentral.nodeAuthHeader}")
    private String nodeAuthHeader;

    private DataNodeService dataNodeService;
    private ApplicationContext applicationContext;

    @Autowired
    public CentralSystemRequestInterceptor(ApplicationContext applicationContext) {

        this.applicationContext = applicationContext;
    }

    private String getNodeToken() {

        init();
        return dataNodeService
                .findCurrentDataNode()
                .orElse(new DataNode())
                .getToken();
    }

    @Override
    public void apply(RequestTemplate template) {

        template.header(nodeAuthHeader, getNodeToken());
    }

    private void init() {

        if (Objects.isNull(this.dataNodeService)) {
            this.dataNodeService = applicationContext.getBean(DataNodeService.class);
        }
    }
}
