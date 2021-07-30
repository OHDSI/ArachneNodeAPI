package com.odysseusinc.arachne.datanode.service.client;

import feign.Client;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.internal.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

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

    @Value("${server.ssl.strictMode}")
    private Boolean sslStrictMode;

    public Client build() {

        return new feign.okhttp.OkHttpClient(buildOkHttpClient(proxyEnabled));
    }

    public Client build(boolean proxyEnabled) {

        return new feign.okhttp.OkHttpClient(buildOkHttpClient(proxyEnabled));
    }

    public static TrustManager[] getTrustAllCertsManager() {

        return new TrustManager[]{
                new X509TrustManager() {

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {

                        return new java.security.cert.X509Certificate[]{};
                    }

                    @Override
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {

                    }

                    @Override
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {

                    }
                }
        };
    }

    public static SSLSocketFactory getTrustAllSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, getTrustAllCertsManager(), new java.security.SecureRandom());
        return sc.getSocketFactory();
    }

    protected OkHttpClient buildOkHttpClient(boolean proxyEnabled) {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
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

        if (!sslStrictMode) {
            try {
                SSLSocketFactory sslSocketFactory = getTrustAllSSLSocketFactory();

                // We cannot use Platform.get() method, since `private static Platform findPlatform()` has bug and determinate Oracle version wrong. Instead we use new Platform() for Oracle/Open JDK 8
                final X509TrustManager trustManager;
                try {
                    Platform platform = new Platform();
                    Class<? extends Platform> platformClass = platform.getClass();
                    Method trustManagerMethod = platformClass.getDeclaredMethod("trustManager", SSLSocketFactory.class);
                    trustManagerMethod.setAccessible(true);
                    trustManager = (X509TrustManager) trustManagerMethod.invoke(platform, sslSocketFactory);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                //final X509TrustManager trustManager = platform.trustManager(sslSocketFactory);
                builder.sslSocketFactory(sslSocketFactory, trustManager);
                HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
                builder.hostnameVerifier((hostname, session) -> true);
            } catch (KeyManagementException | NoSuchAlgorithmException ex) {
                LOGGER.error("Cannot disable strict SSL mode", ex);
            }
        }

        return builder.build();
    }
}
