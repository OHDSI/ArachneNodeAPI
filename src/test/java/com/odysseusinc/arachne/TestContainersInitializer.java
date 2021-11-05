package com.odysseusinc.arachne;

import java.util.stream.Stream;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

public class TestContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>();

    private static void startContainers() {
        Startables.deepStart(Stream.of(
                postgres
                // we can add further containers here, e.g. solr
        )).join();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        startContainers();
        MapPropertySource testcontainers = new MapPropertySource("testcontainers", ImmutableMap.of(
                "spring.datasource.url", postgres.getJdbcUrl(),
                "spring.datasource.username", postgres.getUsername(),
                "spring.datasource.password", postgres.getPassword()
        ));
        context.getEnvironment().getPropertySources().addFirst(testcontainers);
    }
}
