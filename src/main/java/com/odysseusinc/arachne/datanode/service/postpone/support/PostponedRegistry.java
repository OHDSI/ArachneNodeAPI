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
 * Created: Jul 26, 2019
 *
 */

package com.odysseusinc.arachne.datanode.service.postpone.support;

import com.odysseusinc.arachne.datanode.service.postpone.annotation.Postponed;
import com.odysseusinc.arachne.datanode.service.postpone.annotation.PostponedArgument;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PostponedRegistry {

    private Map<Class, PostponedBeanInfo> beanRegistry = new HashMap<>();

    private final ApplicationContext applicationContext;

    public PostponedRegistry(ApplicationContext applicationContext) {

        this.applicationContext = applicationContext;
    }

    public void register(Class type, Object bean, Method method) {

        Postponed postponed = method.getAnnotation(Postponed.class);
        if (postponed == null) {
            throw new IllegalArgumentException("Method should be annotated as Postponed");
        }
        PostponedBeanInfo beanInfo = beanRegistry.getOrDefault(type, new PostponedBeanInfo(bean));
        beanInfo.register(postponed.action(), method);
        beanRegistry.put(type, beanInfo);
    }

    public PostponedMethodInfo getPostponedMethodInfo(Class type, String action) {

        PostponedBeanInfo beanInfo = beanRegistry.get(type);
        if (beanInfo != null) {
            return beanInfo.getPostponedMethodInfo(action);
        }
        return null;
    }

    class PostponedBeanInfo {

        private Object bean;

        public PostponedBeanInfo(Object bean) {

            this.bean = bean;
        }

        private Map<String, PostponedMethodInfo> methodRegistry = new HashMap<>();

        public void register(String action, Method method) {

            Postponed postponed = method.getAnnotation(Postponed.class);
            Parameter[] params = method.getParameters();
            List<PostponedParamInfo> paramInfos = Arrays.stream(params)
                    .map(p -> {
                        PostponedArgument pa = p.getAnnotation(PostponedArgument.class);
                        Converter serializer = new PostponedArgument.DEFAULT_CONVERTER();
                        Converter deserializer = new PostponedArgument.DEFAULT_CONVERTER();
                        if (Objects.nonNull(pa)) {
                            serializer = resolveConverter(pa.serializer());
                            deserializer = resolveConverter(pa.deserializer());
                        }
                        return new PostponedParamInfo(serializer, deserializer);

                    })
                    .collect(Collectors.toList());
            methodRegistry.put(action, new PostponedMethodInfo(bean, method, postponed.defaultReturnValue(), paramInfos));
        }

        private Converter resolveConverter(Class<? extends Converter> converterClass) {

            if (converterClass.equals(PostponedArgument.DEFAULT_CONVERTER.class)) {
                return new PostponedArgument.DEFAULT_CONVERTER();
            }
            Converter converter = applicationContext.getBean(converterClass);
            if (Objects.isNull(converter)) {
                throw new IllegalArgumentException(MessageFormat.format("Couldn't find serializer of type {0}", converterClass));
            }
            return converter;
        }

        PostponedMethodInfo getPostponedMethodInfo(String action) {

            return methodRegistry.get(action);
        }
    }

    public static class PostponedMethodInfo {

        Object bean;
        Method method;
        String defaultReturnValue;
        List<PostponedParamInfo> parameters;

        PostponedMethodInfo(Object bean, Method method, String defaultReturnValue, List<PostponedParamInfo> parameters) {

            this.bean = bean;
            this.method = method;
            this.defaultReturnValue = defaultReturnValue;
            this.parameters = parameters;
        }

        public String getDefaultReturnValue() {

            return defaultReturnValue;
        }

        public List<PostponedParamInfo> getParameters() {

            return Collections.unmodifiableList(parameters);
        }

        public Method getMethod() {

            return method;
        }

        public Object getBean() {

            return bean;
        }
    }

    public static class PostponedParamInfo {

        Converter serializer;
        Converter deserializer;

        public PostponedParamInfo(Converter serializer, Converter deserializer) {

            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        public Converter getSerializer() {

            return serializer;
        }

        public Converter getDeserializer() {

            return deserializer;
        }

    }
}
