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

package com.odysseusinc.arachne.datanode.service.postpone.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.datanode.exception.DataNodeNotRegisteredException;
import com.odysseusinc.arachne.datanode.model.datanode.PostponedRequest;
import com.odysseusinc.arachne.datanode.model.datanode.PostponedRequestState;
import com.odysseusinc.arachne.datanode.repository.PostponedRequestRepository;
import com.odysseusinc.arachne.datanode.service.AuthenticationService;
import com.odysseusinc.arachne.datanode.service.DataNodeService;
import com.odysseusinc.arachne.datanode.service.postpone.PostponeService;
import com.odysseusinc.arachne.datanode.service.postpone.support.PostponedRegistry;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PostponeServiceImpl implements PostponeService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(PostponeService.class);
    private ApplicationContext applicationContext;
    private final PostponedRequestRepository requestRepository;
    private final PostponedRegistry registry;
    private AuthenticationService authenticationService;
    private final DataNodeService dataNodeService;
    @Value("${postponed.retry.maxAttempts}")
    private int maxAttempts;

    public PostponeServiceImpl(ApplicationContext applicationContext,
                               PostponedRequestRepository requestRepository,
                               PostponedRegistry registry,
                               DataNodeService dataNodeService) {

        this.applicationContext = applicationContext;

        this.requestRepository = requestRepository;
        this.registry = registry;
        this.dataNodeService = dataNodeService;
    }

    @Override
    public PostponedRequest saveRequest(Class type, String action, Object[] args) throws IOException {

        PostponedRequest request = new PostponedRequest();
        request.setAction(action);
        request.setObjectClass(type.getName());
        request.setCreatedAt(new Date());
        request.setUsername(authenticationService.getCurrentUserName());
        try(StringWriter writer = new StringWriter()) {
            ObjectMapper mapper = resolveObjectMapper();
            mapper.writeValue(writer, args);
            request.setArgs(writer.toString());
        }
        request.setState(PostponedRequestState.POSTPONED);
        return requestRepository.save(request);
    }

    private ObjectMapper resolveObjectMapper() {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    @Override
    @Async
    public void executePostponedRequests() {

        log.info("Starting execution of postponed requests");
        List<PostponedRequest> requests = requestRepository.findAllByStateOrderByCreatedAt(PostponedRequestState.POSTPONED);
        executeRequests(requests);
        log.info("Execution of {} postponed requests has finished", requests.size());
    }

    @Override
    @Async
    public void retryFailedRequests() {

        log.info("Retrying to execute previously failed postponed requests");
        List<PostponedRequest> requests = requestRepository.findAllByStateOrderByCreatedAt(PostponedRequestState.SENT_ERROR);
        executeRequests(requests);
        log.info("Retrying of {} postponed requests finished", requests.size());
    }

    protected void executeRequests(List<PostponedRequest> requests) {

        requests.forEach(r -> {
            try {
                if (Objects.nonNull(r.getUsername())) {
                    String systemToken = dataNodeService.findCurrentDataNode().orElseThrow(() -> new DataNodeNotRegisteredException("DataNode is not registered")).getToken();
                    authenticationService.impersonate(systemToken, r.getUsername());
                }
                Class<?> serviceClass = Class.forName(r.getObjectClass());
                PostponedRegistry.PostponedMethodInfo methodInfo = registry.getPostponedMethodInfo(serviceClass, r.getAction());
                if (Objects.nonNull(methodInfo)) {
                    Method method = methodInfo.getMethod();
                    String args = r.getArgs();
                    ObjectMapper mapper = resolveObjectMapper();
                    Object[] argValues = mapper.readValue(args, Object[].class);
                    Object bean = methodInfo.getBean();
                    if (Objects.isNull(bean)) {
                        throw new IllegalArgumentException(MessageFormat.format("Bean of type <{0}> not found in context", serviceClass));
                    }
                    if (log.isDebugEnabled()) {
                        int attempt = getRetries(r) + 1;
                        log.debug("Executing {}:{}, attempt #: {}", serviceClass, method.getName(), attempt);
                    }
                    method.invoke(bean, deserializeArguments(methodInfo, argValues));
                    completeRequest(r);
                } else {
                    throw new IllegalArgumentException("Request method info not found in registry");
                }
            } catch (Exception e) {
                log.error("Failed to execute postponed request {}/{}", r.getObjectClass(), r.getAction(), e);
                failAttempt(r, e);
            }
        });
    }

    private int getRetries(PostponedRequest request) {
        return Objects.nonNull(request.getRetries()) ? request.getRetries() : 0;
    }

    private void failAttempt(PostponedRequest request, Throwable reason) {

        int retries = getRetries(request);
        if (maxAttempts > 0 && retries > maxAttempts) {
            request.setReason(MessageFormat.format("Max attempts ({0}) has been reached", maxAttempts));
            request.setState(PostponedRequestState.CANCELED);
        } else {
            request.setRetries(retries + 1);
            request.setReason(reason.getMessage());
            request.setState(PostponedRequestState.SENT_ERROR);
        }
        request.setLastSent(new Date());
        requestRepository.save(request);
    }

    private void completeRequest(PostponedRequest request) {

        request.setLastSent(new Date());
        request.setState(PostponedRequestState.SENT);
        requestRepository.save(request);
    }

    private Object[] deserializeArguments(PostponedRegistry.PostponedMethodInfo methodInfo, Object[] args) {

        Object[] result = new Object[args.length];
        final List<Converter> deserializers = methodInfo.getParameters().stream()
                .map(PostponedRegistry.PostponedParamInfo::getDeserializer)
                .collect(Collectors.toList());
        for(int i = 0; i < args.length; i++) {
            result[i] = deserializers.get(i).convert(args[i]);
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        authenticationService = applicationContext.getBean(AuthenticationService.class);
    }
}
