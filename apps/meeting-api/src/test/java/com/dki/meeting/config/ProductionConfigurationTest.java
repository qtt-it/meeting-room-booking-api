package com.dki.meeting.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationTest {
  @Test
  void actualProductionProfileDefaultsDisableDocsAndBootstrap() {
    new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(ProductionConfiguration.class, PlatformConfiguration.class)
        .withPropertyValues(
            "spring.profiles.active=prod",
            "DATABASE_URL=jdbc:postgresql://db.internal/meeting_booking?sslmode=verify-full",
            "DATABASE_USERNAME=meeting_runtime",
            "DATABASE_PASSWORD=PrivateRuntimeSecret2026!",
            "JWT_SECRET=PrivateSigningKeyForProfileTestsOnly2026",
            "CORS_ALLOWED_ORIGINS=https://app.example.com",
            "FLYWAY_ENABLED=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed().doesNotHaveBean("bootstrap");
              assertThat(context.getEnvironment().getProperty("app.docs.enabled", Boolean.class))
                  .isFalse();
              assertThat(
                      context
                          .getEnvironment()
                          .getProperty("springdoc.api-docs.enabled", Boolean.class))
                  .isFalse();
              assertThat(
                      context.getEnvironment().getProperty("app.bootstrap.enabled", Boolean.class))
                  .isFalse();
            });
  }

  @Test
  void acceptsExplicitSecureConfiguration() {
    assertThatCode(() -> ProductionConfiguration.validate(valid())).doesNotThrowAnyException();
  }

  @Test
  void validationRunsAtStartupBeforeOtherBeans() {
    new ApplicationContextRunner()
        .withUserConfiguration(ProductionConfiguration.class)
        .withPropertyValues("spring.profiles.active=prod")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasMessageContaining("spring.datasource.url"));
  }

  @Test
  void rejectsWeakOrKnownDemoSecretsWithoutPrintingTheValue() {
    for (String key :
        new String[] {
          "spring.datasource.password", "spring.flyway.password", "app.security.jwt-secret"
        }) {
      String sample = "local-development-key-replace-before-any-deployment-2026";
      assertThatThrownBy(() -> ProductionConfiguration.validate(valid().withProperty(key, sample)))
          .hasMessageContaining(key)
          .hasMessageNotContaining(sample);
      assertThatThrownBy(() -> ProductionConfiguration.validate(valid().withProperty(key, "short")))
          .hasMessageContaining(key)
          .hasMessageNotContaining("short");
    }
  }

  @Test
  void rejectsInsecureOrAmbiguousDatabaseTlsSettings() {
    for (String suffix :
        new String[] {
          "?sslmode=require",
          "?sslmode=verify-full&sslmode=disable",
          "?sslmode=verify-full&sslfactory=org.postgresql.ssl.NonValidatingFactory",
          "?sslmode=verify-full&password=secret",
          "?sslmode=verify-full&sslhostnameverifier=custom"
        }) {
      assertThatThrownBy(
              () ->
                  ProductionConfiguration.validate(
                      valid()
                          .withProperty(
                              "spring.datasource.url",
                              "jdbc:postgresql://db.internal/meeting_booking" + suffix)))
          .hasMessageContaining("spring.datasource.url");
    }
  }

  @Test
  void rejectsUnsafeCorsOrigins() {
    for (String origin :
        new String[] {
          "*",
          "http://app.example.com",
          "https://localhost",
          "https://app.example.com/path",
          "https://app.example.com,",
          "https://user:secret@app.example.com"
        }) {
      assertThatThrownBy(
              () ->
                  ProductionConfiguration.validate(
                      valid().withProperty("app.security.allowed-origins", origin)))
          .hasMessageContaining("app.security.allowed-origins");
    }
  }

  @Test
  void rejectsPoolAndMigrationPropertiesThatCouldBypassTheValidatedSettings() {
    for (String key :
        new String[] {
          "spring.datasource.hikari.jdbc-url",
          "spring.datasource.hikari.username",
          "spring.datasource.hikari.data-source-properties.sslmode",
          "spring.flyway.jdbc-properties.sslhostnameverifier"
        })
      assertThatThrownBy(
              () -> ProductionConfiguration.validate(valid().withProperty(key, "override")))
          .hasMessageContaining(key);
  }

  @Test
  void requiresADistinctMigrationRoleOnlyWhenMigrationsAreEnabled() {
    assertThatThrownBy(
            () ->
                ProductionConfiguration.validate(
                    valid().withProperty("spring.flyway.user", "meeting_runtime")))
        .hasMessageContaining("spring.flyway.user");
    assertThatCode(
            () ->
                ProductionConfiguration.validate(
                    valid()
                        .withProperty("spring.flyway.enabled", "false")
                        .withProperty("spring.flyway.user", "")
                        .withProperty("spring.flyway.password", "")))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsPublicDocumentationOverrides() {
    for (String key :
        new String[] {
          "app.docs.enabled", "springdoc.api-docs.enabled", "springdoc.swagger-ui.enabled"
        })
      assertThatThrownBy(() -> ProductionConfiguration.validate(valid().withProperty(key, "true")))
          .hasMessageContaining(key);
  }

  @Test
  void requiresExplicitBootstrapAndRemovalOfUnusedCredentials() {
    var environment =
        valid()
            .withProperty("app.bootstrap.email", "admin@example.com")
            .withProperty("app.bootstrap.password", "PrivateBootstrap!2026");
    assertThatThrownBy(() -> ProductionConfiguration.validate(environment))
        .hasMessageContaining("app.bootstrap.enabled");
    assertThatCode(
            () ->
                ProductionConfiguration.validate(
                    environment.withProperty("app.bootstrap.enabled", "true")))
        .doesNotThrowAnyException();
  }

  private MockEnvironment valid() {
    String url =
        "jdbc:postgresql://db.internal/meeting_booking?sslmode=verify-full&sslrootcert=/run/secrets/postgres-ca.crt";
    return new MockEnvironment()
        .withProperty("spring.datasource.url", url)
        .withProperty("spring.datasource.username", "meeting_runtime")
        .withProperty("spring.datasource.password", "RuntimeSecret!2026Private")
        .withProperty("spring.flyway.url", url)
        .withProperty("spring.flyway.user", "meeting_migrator")
        .withProperty("spring.flyway.password", "MigrationSecret!2026Private")
        .withProperty("app.security.jwt-secret", "PrivateSigningKeyForProductionTestsOnly2026")
        .withProperty("app.security.allowed-origins", "https://app.example.com")
        .withProperty("app.docs.enabled", "false")
        .withProperty("springdoc.api-docs.enabled", "false")
        .withProperty("springdoc.swagger-ui.enabled", "false");
  }
}
