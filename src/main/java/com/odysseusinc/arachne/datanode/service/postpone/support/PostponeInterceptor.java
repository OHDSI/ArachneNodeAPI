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

package com.odysseusinc.arachne.datanode.service.postpone.support;

import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.postpone.PostponeService;
import com.odysseusinc.arachne.datanode.service.postpone.annotation.Postponed;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

@Component
public class PostponeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PostponeService.class);

    private final DataNodeService dataNodeService;
    private final PostponeService postponeService;

    public PostponeInterceptor(DataNodeService dataNodeService,
                               PostponeService postponeService) {

        this.dataNodeService = dataNodeService;
        this.postponeService = postponeService;
    }

    public Object invokeAsPostponed(Class type, Object bean, Method method, Object[] args) {

        if (Objects.equals(dataNodeService.getDataNodeMode(), FunctionalMode.NETWORK)) {
            try {
                return method.invoke(bean, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Failed to invoke method", e);
                throw new RuntimeException(e);
            }
        } else {
            Postponed annotation = method.getAnnotation(Postponed.class);
            try {
                postponeService.saveRequest(type, annotation.action(), args);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String returnValue = annotation.defaultReturnValue();
            ExpressionParser parser = new SpelExpressionParser();
            return parser.parseExpression(returnValue).getValue();
        }
    }
}
