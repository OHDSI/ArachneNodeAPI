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

import static java.util.Objects.requireNonNull;

import com.odysseusinc.arachne.datanode.model.datanode.FunctionalMode;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.postpone.PostponeService;
import com.odysseusinc.arachne.datanode.service.postpone.annotation.Postponed;
import com.odysseusinc.arachne.datanode.service.postpone.annotation.PostponedArgument;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class PostponeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PostponeService.class);

    private final DataNodeService dataNodeService;
    private final PostponeService postponeService;
    private final ApplicationContext applicationContext;
    private final PostponedRegistry postponedRegistry;

    public PostponeInterceptor(DataNodeService dataNodeService,
                               PostponeService postponeService,
                               ApplicationContext applicationContext,
                               PostponedRegistry postponedRegistry) {

        this.dataNodeService = dataNodeService;
        this.postponeService = postponeService;
        this.applicationContext = applicationContext;
        this.postponedRegistry = postponedRegistry;
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
            Postponed postponed = method.getAnnotation(Postponed.class);
            if (Objects.isNull(postponed)) {
                Method instanceMethod = ReflectionUtils.findMethod(type, method.getName(), method.getParameterTypes());
                postponed = instanceMethod.getDeclaredAnnotation(Postponed.class);
            }
            PostponedRegistry.PostponedMethodInfo methodInfo = postponedRegistry.getPostponedMethodInfo(type, postponed.action());
            try {
                Object[] vals = serializeArguments(methodInfo, args);
                postponeService.saveRequest(type, postponed.action(), vals);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String returnValue = postponed.defaultReturnValue();
            ExpressionParser parser = new SpelExpressionParser();
            return parser.parseExpression(returnValue).getValue();
        }
    }

    private Object[] serializeArguments(PostponedRegistry.PostponedMethodInfo methodInfo, Object[] args) {

        Object[] result = new Object[args.length];
        final List<Converter> serializers = methodInfo.getParameters().stream()
                .map(PostponedRegistry.PostponedParamInfo::getSerializer)
                .collect(Collectors.toList());

        for(int i = 0; i < args.length; i++) {
            result[i] = serializers.get(i).convert(args[i]);
        }

        return result;
    }
}
