package com.odysseusinc.arachne.datanode.service.impl;

import static com.odysseusinc.arachne.datanode.Constants.Atlas.ATLAS_2_7_VERSION;

import com.fasterxml.jackson.databind.Module;
import com.odysseusinc.arachne.datanode.dto.serialize.PageModule;
import com.odysseusinc.arachne.datanode.exception.AtlasAuthException;
import com.odysseusinc.arachne.datanode.model.atlas.Atlas;
import com.odysseusinc.arachne.datanode.service.AtlasClientService;
import com.odysseusinc.arachne.datanode.service.client.ArachneHttpClientBuilder;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_5;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasClient2_7;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasInfoClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasLoginClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.GoogleLoginClient;
import com.odysseusinc.arachne.datanode.service.client.atlas.TokenDecoder;
import com.odysseusinc.arachne.datanode.service.client.decoders.ByteArrayDecoder;
import feign.Client;
import feign.Feign;
import feign.Retryer;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AtlasClientServiceImpl implements AtlasClientService {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_FORMAT = "Bearer %s";
    private static final String ATLAS_LOWER_VERSION = "2.2.0";
    private static final List<Module> MODULES = Collections.singletonList(new PageModule());

    private final ArachneHttpClientBuilder arachneHttpClientBuilder;
    private final GoogleLoginClient googleLoginClient;

    @Autowired
    public AtlasClientServiceImpl(ArachneHttpClientBuilder arachneHttpClientBuilder, GoogleLoginClient googleLoginClient) {

        this.arachneHttpClientBuilder = arachneHttpClientBuilder;
        this.googleLoginClient = googleLoginClient;
    }

    @Override
    public <T extends AtlasClient> T buildAtlasClient(Atlas atlas) {

        Client httpClient = arachneHttpClientBuilder.build();
        AtlasLoginClient atlasLoginClient = buildAtlasLoginClient(atlas.getUrl(), httpClient);
        return Feign.builder()
                .client(httpClient)
                .encoder(new JacksonEncoder())
                .decoder(new ByteArrayDecoder(new JacksonDecoder(MODULES)))
                .logger(new Slf4jLogger(AtlasClient.class))
                .logLevel(feign.Logger.Level.FULL)
                .requestInterceptor(template -> authToAtlas(atlas, atlasLoginClient)
                        .ifPresent(token -> template.header(
                                AUTHORIZATION_HEADER,
                                String.format(BEARER_FORMAT, token))
                        )
                )
                .target(getAtlasClientClass(atlas), atlas.getUrl());
    }

    @Override
    public AtlasInfoClient buildAtlasInfoClient(Atlas atlas) {

        Client httpClient = arachneHttpClientBuilder.build();
        return Feign.builder()
                .client(httpClient)
                .retryer(Retryer.NEVER_RETRY)
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(AtlasInfoClient.class))
                .logLevel(feign.Logger.Level.BASIC)
                .target(AtlasInfoClient.class, atlas.getUrl());
    }

    @Override
    public AtlasLoginClient buildAtlasLoginClient(String url, Client httpClient) {

        return Feign.builder()
                .client(httpClient)
                .encoder(new FormEncoder(new JacksonEncoder()))
                .decoder(new TokenDecoder())
                .logger(new Slf4jLogger(AtlasLoginClient.class))
                .logLevel(feign.Logger.Level.BASIC)
                .target(AtlasLoginClient.class, url);
    }

    private Optional<String> authToAtlas(Atlas atlas, AtlasLoginClient atlasLoginClient) {

        if (Objects.isNull(atlas.getAuthType())) {
            throw new AtlasAuthException("Atlas token is null");
        }

        try {
            switch (atlas.getAuthType()) {
                case DATABASE:
                    return Optional.of(atlasLoginClient.loginDatabase(atlas.getUsername(), atlas.getPassword()));
                case LDAP:
                    return Optional.of(atlasLoginClient.loginLdap(atlas.getUsername(), atlas.getPassword()));
                case ACCESS_TOKEN:
                    return Optional.of(googleLoginClient.generateJWTToken(atlas.getKeyfile()));
                case NONE:
                    return Optional.empty();
                default:
                    throw new AtlasAuthException("Unsupported authentication type");
            }
        } catch (IllegalArgumentException e) {
            throw new AtlasAuthException("Unsupported authentication type");
        } catch (Exception e) {
            throw new AtlasAuthException("Atlas auth error");
        }
    }

    private <T extends AtlasClient> Class<T> getAtlasClientClass(Atlas atlas) {

        String version = Objects.nonNull(atlas) && Objects.nonNull(atlas.getVersion()) ? atlas.getVersion() : ATLAS_LOWER_VERSION;
        return ATLAS_2_7_VERSION.isLesserOrEqualsThan(version) ? (Class<T>) AtlasClient2_7.class : (Class<T>) AtlasClient2_5.class;
    }
}
