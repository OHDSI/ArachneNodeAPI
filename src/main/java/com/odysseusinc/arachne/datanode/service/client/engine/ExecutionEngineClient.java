package com.odysseusinc.arachne.datanode.service.client.engine;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.datanode.service.client.ArachneHttpClientBuilder;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.RuntimeEnvironmentDescriptorsDTO;
import dev.failsafe.RetryPolicy;
import dev.failsafe.okhttp.FailsafeCall;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExecutionEngineClient {

    private static final String EMPTY_FILENAME = "";
    public static final okhttp3.MediaType APPLICATION_JSON = okhttp3.MediaType.parse("application/json");
    private final OkHttpClient httpClient;
    private final EngineClientConfig properties;
    private final ObjectMapper objectMapper;

    public ExecutionEngineClient(
            ArachneHttpClientBuilder clientBuilder, EngineClientConfig properties, ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.httpClient = clientBuilder.buildOkHttpClient(properties.getProxyEnabledForEngine());
        this.objectMapper = objectMapper;
    }

    public AnalysisRequestStatusDTO sendAnalysisRequest(
            AnalysisRequestDTO analysisRequest,
            File file,
            boolean compressedResult,
            boolean healthCheck) {
        String json = toJson(analysisRequest);
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("analysisRequest", EMPTY_FILENAME, RequestBody.create(json, APPLICATION_JSON))
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, okhttp3.MediaType.parse("application/octet-stream")))
                .build();
        String url = buildUrl(properties.getAnalysisUri());
        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .header("arachne-compressed", "true")
                .header("arachne-waiting-compressed-result", Boolean.toString(compressedResult))
                .header("arachne-datasource-check", Boolean.toString(healthCheck))
                .post(body)
                .build();
        Call call = httpClient.newCall(request);
        FailsafeCall failsafeCall = FailsafeCall.with(retryPolicy()).compose(call);
        return executeRequest(url, failsafeCall, AnalysisRequestStatusDTO.class);
    }

    public Optional<Supplier<RuntimeEnvironmentDescriptorsDTO>> getDescriptors() {
        Optional<String> uri = Optional.ofNullable(properties.getDescriptorsUri());
        if (!uri.isPresent()) {
            log.warn("Descriptor support is not configured (set property 'execution.engine.descriptorsUrl' to enable)");
        }
        return uri.map(this::buildUrl).map(url -> () -> {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            Call call = httpClient.newCall(request);
            FailsafeCall failsafeCall = FailsafeCall.with(retryPolicy()).compose(call);
            return executeRequest(url, failsafeCall, RuntimeEnvironmentDescriptorsDTO.class);
        });
    }

    private <T> T executeRequest(String url, FailsafeCall call, Class<T> valueType) {
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to execute to [{}]. Response code: [{}]", url, response.code());
                throw new AnalysisExecutionException("Failed to get descriptors. Response code: " + response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody != null && Objects.equals(responseBody.contentType(), APPLICATION_JSON)) {
                return objectMapper.readValue(responseBody.byteStream(), valueType);
            } else {
                String contentType = Optional.ofNullable(responseBody).map(ResponseBody::contentType).map(okhttp3.MediaType::type).orElse(null);
                log.error("Request to [{}] returned unsupported content type: [{}]", url, contentType);
                throw new AnalysisExecutionException("Unexpected response: " + contentType);
            }
        } catch (IOException e) {
            log.error("Request to [{}] failed: {}", e.getMessage(), e);
            throw new AnalysisExecutionException("Failed to get descriptors from Execution Engine: " + e.getMessage(), e);
        }
    }

    private String buildUrl(String suffix) {
        return String.format("%s://%s:%s%s", properties.getProtocol(), properties.getHost(), properties.getPort(), suffix);
    }

    private String toJson(AnalysisRequestDTO analysisRequest) {
        try {
            return objectMapper.writeValueAsString(analysisRequest);
        } catch (IOException e) {
            log.error("Failed to prepare analysis request", e);
            throw new AnalysisExecutionException("Failed to prepare analysis request: " + e.getMessage());
        }
    }

    private RetryPolicy<Response> retryPolicy() {
        return RetryPolicy.<Response>builder()
                .withMaxAttempts(2)
                .withBackoff(2, 4, ChronoUnit.SECONDS)
                .build();
    }
}
