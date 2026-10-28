package com.dki.meeting.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
@Profile("prod")
public class ProductionConfiguration {
  @Bean
  static BeanFactoryPostProcessor productionSettings(Environment environment) {
    return beans -> validate(environment);
  }

  static void validate(Environment environment) {
    String database = required(environment, "spring.datasource.url");
    validateDatabaseUrl(database);
    for (String key :
        List.of(
            "spring.datasource.hikari.jdbc-url",
            "spring.datasource.hikari.username",
            "spring.datasource.hikari.password"))
      if (environment.getProperty(key) != null) fail(key);
    for (String prefix :
        List.of(
            "spring.datasource.hikari.data-source-properties.", "spring.flyway.jdbc-properties."))
      for (String key :
          List.of(
              "sslmode",
              "ssl",
              "sslfactory",
              "sslfactoryarg",
              "sslhostnameverifier",
              "user",
              "password")) if (environment.getProperty(prefix + key) != null) fail(prefix + key);
    String runtimeUser = required(environment, "spring.datasource.username");
    secret(environment, "spring.datasource.password", 16);
    secret(environment, "app.security.jwt-secret", 32);
    String origins = required(environment, "app.security.allowed-origins");
    for (String origin : origins.split(",", -1)) validateOrigin(origin.trim());
    for (String key :
        List.of("app.docs.enabled", "springdoc.api-docs.enabled", "springdoc.swagger-ui.enabled"))
      if (environment.getProperty(key, Boolean.class, true)) fail(key);

    if (environment.getProperty("spring.flyway.enabled", Boolean.class, true)) {
      if (!required(environment, "spring.flyway.url").equals(database)) fail("spring.flyway.url");
      String migrationUser = required(environment, "spring.flyway.user");
      if (migrationUser.equalsIgnoreCase(runtimeUser)) fail("spring.flyway.user");
      secret(environment, "spring.flyway.password", 16);
    }
    boolean bootstrap = environment.getProperty("app.bootstrap.enabled", Boolean.class, false);
    String email = environment.getProperty("app.bootstrap.email", "");
    String password = environment.getProperty("app.bootstrap.password", "");
    if (bootstrap) {
      if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) fail("app.bootstrap.email");
      secret(environment, "app.bootstrap.password", 12);
      if (password.getBytes(StandardCharsets.UTF_8).length > 72) fail("app.bootstrap.password");
    } else if (!email.isBlank() || !password.isBlank()) {
      fail("app.bootstrap.enabled (remove unused bootstrap credentials)");
    }
  }

  private static void validateDatabaseUrl(String database) {
    try {
      if (!database.startsWith("jdbc:postgresql://")) fail("spring.datasource.url");
      URI uri = URI.create(database.substring(5));
      if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null)
        fail("spring.datasource.url");
      var parameters = new HashMap<String, String>();
      for (String entry : Optional.ofNullable(uri.getRawQuery()).orElse("").split("&")) {
        String[] pair = entry.split("=", 2);
        String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        String value = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
        if (parameters.put(key, value) != null) fail("spring.datasource.url");
      }
      if (!"verify-full".equals(parameters.get("sslmode"))) fail("spring.datasource.url");
      if (parameters.keySet().stream()
          .anyMatch(
              Set.of("sslfactory", "sslfactoryarg", "sslhostnameverifier", "user", "password")
                  ::contains)) fail("spring.datasource.url");
    } catch (IllegalArgumentException failure) {
      fail("spring.datasource.url");
    }
  }

  private static void validateOrigin(String origin) {
    try {
      URI uri = URI.create(origin);
      if (!"https".equals(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getQuery() != null
          || uri.getFragment() != null
          || !uri.getPath().isEmpty()
          || uri.getHost().equalsIgnoreCase("localhost")
          || uri.getHost().startsWith("127.")
          || uri.getHost().equals("[::1]")) fail("app.security.allowed-origins");
    } catch (IllegalArgumentException failure) {
      fail("app.security.allowed-origins");
    }
  }

  private static String required(Environment environment, String key) {
    String value = environment.getProperty(key, "");
    if (value.isBlank()) fail(key);
    return value;
  }

  private static void secret(Environment environment, String key, int minimumBytes) {
    String value = required(environment, key);
    String normalized = value.toLowerCase(Locale.ROOT);
    if (value.getBytes(StandardCharsets.UTF_8).length < minimumBytes
        || normalized.contains("local-development")
        || normalized.contains("local_database_password")
        || normalized.contains("localdemo")
        || normalized.contains("change-me")
        || normalized.contains("changeme")
        || normalized.contains("${")
        || value.startsWith("<")) fail(key);
  }

  private static void fail(String key) {
    throw new IllegalStateException("Unsafe or missing production setting: " + key);
  }
}
