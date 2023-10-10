package com.odysseusinc.arachne.datanode.service.client.engine;

import com.odysseusinc.arachne.datanode.service.client.ArachneHttpClientBuilder;
import com.odysseusinc.arachne.datanode.service.client.FeignSpringFormEncoder;
import com.odysseusinc.arachne.datanode.util.RestUtils;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.StringDecoder;
import feign.jackson.JacksonDecoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
    @Primary
    public EngineClient engineClient(){
        return getEngineClient(new JacksonDecoder());
    }

    @Bean("engineStatusClient")
    public EngineClient engineStatusClient(){
        return getEngineClient(new StringDecoder());
    }
    
    private EngineClient getEngineClient(Decoder decoder) {
        String url = String.format("%s://%s:%s",
                executionEngineProtocol,
                executionEngineHost,
                executionEnginePort
        );
        return Feign.builder()
                .client(arachneHttpClientBuilder.build(proxyEnabledForEngine))
                .encoder(new FeignSpringFormEncoder())
                .decoder(decoder)
                .requestInterceptor(rt -> rt.header("Authorization", RestUtils.checkCredentials(executionEngineToken)))
                .logger(new Slf4jLogger(EngineClient.class))
                .logLevel(feign.Logger.Level.FULL)
                .target(EngineClient.class, url);
    }
}
