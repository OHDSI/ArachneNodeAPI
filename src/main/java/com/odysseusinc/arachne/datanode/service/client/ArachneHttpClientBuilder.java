package com.odysseusinc.arachne.datanode.service.client;

import feign.Client;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ArachneHttpClientBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArachneHttpClientBuilder.class);

    @Value("${proxy.enabled}")
    private Boolean proxyEnabled;
    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private Integer proxyPort;
    @Value("${proxy.auth.enabled}")
    private Boolean proxyAuthEnabled;
    @Value("${proxy.auth.username}")
    private String proxyUsername;
    @Value("${proxy.auth.password}")
    private String proxyPassword;

    public Client build() {

        return new feign.okhttp.OkHttpClient(buildOkHttpClient());
    }

    protected OkHttpClient buildOkHttpClient() {

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS);

        if (proxyEnabled) {

            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));

            if (proxyAuthEnabled) {
                Authenticator proxyAuthenticator = (route, response) -> {
                    String credential = Credentials.basic(proxyUsername, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                };
                builder.proxyAuthenticator(proxyAuthenticator);
                LOGGER.info("Using proxy with authentication for Feign client");
            } else {
                LOGGER.info("Using proxy for Feign client");
            }
        }

        return builder.build();
    }
}
