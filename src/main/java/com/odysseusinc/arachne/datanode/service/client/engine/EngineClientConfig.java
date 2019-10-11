package com.odysseusinc.arachne.datanode.service.client.engine;

import com.odysseusinc.arachne.datanode.service.client.ArachneHttpClientBuilder;
import com.odysseusinc.arachne.datanode.util.RestUtils;
import com.odysseusinc.arachne.execution_engine_common.client.FeignSpringFormEncoder;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineClientConfig {

    @Value("${executionEngine.protocol}")
    private String executionEngineProtocol;
    @Value("${executionEngine.host}")
    private String executionEngineHost;
    @Value("${executionEngine.port}")
    private String executionEnginePort;
    @Value("${executionEngine.analysisUri}")
    private String executionEngineAnalysisUri;
    @Value("${executionEngine.token}")
    private String executionEngineToken;
    @Value("${proxy.enabledForEngine}")
    private Boolean proxyEnabledForEngine;

    private final ArachneHttpClientBuilder arachneHttpClientBuilder;

    public EngineClientConfig(ArachneHttpClientBuilder arachneHttpClientBuilder) {

        this.arachneHttpClientBuilder = arachneHttpClientBuilder;
    }

    @Bean
    public EngineClient engineClient(){

        String url = String.format("%s://%s:%s",
                executionEngineProtocol,
                executionEngineHost,
                executionEnginePort
        );
        return Feign.builder()
                .client(arachneHttpClientBuilder.build(proxyEnabledForEngine))
                .encoder(new FeignSpringFormEncoder())
                .decoder(new JacksonDecoder())
                .requestInterceptor(rt -> rt.header("Authorization", RestUtils.checkCredentials(executionEngineToken)))
                .logger(new Slf4jLogger(EngineClient.class))
                .logLevel(feign.Logger.Level.FULL)
                .target(EngineClient.class, url);
    }
}
