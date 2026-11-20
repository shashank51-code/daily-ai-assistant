package com.dailyai.assistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the deployment-critical parts of application.yml without starting a
 * context or touching a database.
 */
class DeploymentConfigurationTest {

    @Test
    void renderPortOverridesServerPortAndKeepsTheLocalDefault() throws IOException {
        MockEnvironment environment = applicationYaml();

        assertThat(environment.getProperty("server.port")).isEqualTo("8080");

        environment.getPropertySources()
                .addFirst(new MapPropertySource("serverPort", Map.of("SERVER_PORT", "9090")));
        assertThat(environment.getProperty("server.port")).isEqualTo("9090");

        environment.getPropertySources()
                .addFirst(new MapPropertySource("renderPort", Map.of("PORT", "10000")));
        assertThat(environment.getProperty("server.port")).isEqualTo("10000");
    }

    @Test
    void forwardedHeadersAreEnabledForRenderTlsTermination() throws IOException {
        assertThat(applicationYaml().getProperty("server.forward-headers-strategy"))
                .isEqualTo("framework");
    }

    @Test
    void flywayNeverBaselinesAnExistingSchema() throws IOException {
        assertThat(applicationYaml().getProperty("spring.flyway.baseline-on-migrate"))
                .isEqualTo("false");
    }

    @Test
    void flywayChecksumValidationIsLeftAtTheSecureDefault() throws IOException {
        // Flyway validates checksums on migrate unless explicitly disabled; an override
        // here would silently let an edited V1-V11 through during the V12 validation.
        assertThat(applicationYaml().getProperty("spring.flyway.validate-on-migrate"))
                .isNull();
    }

    @Test
    void aiStaysDisabledByDefault() throws IOException {
        assertThat(applicationYaml().getProperty("app.ai.enabled")).isEqualTo("false");
    }

    @Test
    void sessionCookieDefaultsRemainSafeForLocalDevelopment() throws IOException {
        MockEnvironment environment = applicationYaml();

        assertThat(environment.getProperty("server.servlet.session.cookie.name"))
                .isEqualTo("DAILYAI_SESSION");
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only"))
                .isEqualTo("true");
        assertThat(environment.getProperty("server.servlet.session.cookie.secure"))
                .isEqualTo("false");
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site"))
                .isEqualTo("lax");
    }

    private static MockEnvironment applicationYaml() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        sources.forEach(environment.getPropertySources()::addLast);
        return environment;
    }
}
