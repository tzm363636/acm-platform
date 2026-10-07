package com.acm.platform.db;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import static org.assertj.core.api.Assertions.assertThat;

class RenderConfigurationTest {
    @Configuration(proxyBeanMethods = false)
    @Profile("aiven")
    static class DatabaseProfileMarker {
        @Bean String databaseProfileMarker() { return "active"; }
    }

    @Test void renderActivatesDatabaseProfileAndHttpsSessionConfiguration() {
        new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=aiven,render", "PORT=12001", "DB_EXPECTED_MAJOR=8", "DB_POOL_MAX=2")
            .withUserConfiguration(DatabaseProfileMarker.class)
            .run(context -> {
                assertThat(context).hasNotFailed().hasBean("databaseProfileMarker");
                var env = context.getEnvironment();
                assertThat(env.getActiveProfiles()).contains("render", "aiven");
                assertThat(env.getProperty("server.address")).isEqualTo("0.0.0.0");
                assertThat(env.getProperty("server.port")).isEqualTo("12001");
                assertThat(env.getProperty("server.servlet.session.cookie.secure")).isEqualTo("true");
                assertThat(env.getProperty("server.servlet.session.cookie.http-only")).isEqualTo("true");
                assertThat(env.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
                assertThat(env.getProperty("acm.db.ssl-mode")).isEqualTo("VERIFY_IDENTITY");
                assertThat(env.getProperty("acm.db.action")).isEqualTo("serve");
            });
    }
}
