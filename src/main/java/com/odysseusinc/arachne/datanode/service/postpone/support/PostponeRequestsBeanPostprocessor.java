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

import com.odysseusinc.arachne.commons.utils.AnnotationReflectionUtils;
import com.odysseusinc.arachne.datanode.service.postpone.annotation.Postponed;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class PostponeRequestsBeanPostprocessor implements BeanPostProcessor {

    private boolean proxyTargetClass;

    private PostponeInterceptor interceptor;

    public PostponeRequestsBeanPostprocessor(boolean proxyTargetClass, PostponeInterceptor interceptor) {

        this.proxyTargetClass = proxyTargetClass;
        this.interceptor = interceptor;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        Class type = bean.getClass();
        List<Method> annotatedMethods = AnnotationReflectionUtils.getMethodsAnnotatedWith(type, Postponed.class);
        Object result = bean;

        if (!annotatedMethods.isEmpty()) {
            ProxyFactory proxyFactory = new ProxyFactory(bean);
            proxyFactory.setProxyTargetClass(proxyTargetClass);
            proxyFactory.addAdvice((MethodInterceptor) invocation -> {
                Method method = invocation.getMethod();
                Object[] args = invocation.getArguments();
                Optional<Method> originalMethod = annotatedMethods.stream().filter(m ->
                        Objects.equals(m.getName(), method.getName())
                                && Objects.equals(m.getReturnType(), method.getReturnType())
                                && Arrays.equals(m.getParameterTypes(), method.getParameterTypes())).findFirst();
                return originalMethod
                        .map(m -> interceptor.invokeAsPostponed(type, bean, method, args))
                        .orElse(method.invoke(bean, args));
            });
            result = proxyFactory.getProxy();
        }

        return result;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        return bean;
    }

}
