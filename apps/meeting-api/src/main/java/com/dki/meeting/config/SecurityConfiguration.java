package com.dki.meeting.config;

import com.dki.meeting.foundation.*;
import com.dki.meeting.foundation.http.JsonSecurityErrors;
import com.dki.meeting.identity.application.service.IdentityService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
  @Bean
  SecretKey signingKey(@Value("${app.security.jwt-secret}") String secret) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey key) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    OAuth2TokenValidator<Jwt> audience =
        jwt ->
            jwt.getAudience().contains("meeting-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience", null));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer("meeting-room-booking"), audience));
    return decoder;
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      IdentityService users,
      JsonSecurityErrors errors,
      @Value("${app.docs.enabled:true}") boolean docs)
      throws Exception {
    http.csrf(c -> c.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a -> {
              a.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
              a.requestMatchers(
                      HttpMethod.POST,
                      "/api/v1/auth/login",
                      "/api/v1/auth/refresh",
                      "/api/v1/auth/logout")
                  .permitAll();
              if (docs)
                a.requestMatchers(
                        "/docs",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml")
                    .permitAll();
              a.requestMatchers("/actuator/**").hasRole("ADMIN").anyRequest().authenticated();
            })
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (q, r, x) -> {
                          r.setHeader("WWW-Authenticate", "Bearer");
                          errors.write(r, 401);
                        })
                    .accessDeniedHandler((q, r, x) -> errors.write(r, 403)))
        .oauth2ResourceServer(
            o ->
                o.authenticationEntryPoint(
                        (q, r, x) -> {
                          r.setHeader("WWW-Authenticate", "Bearer");
                          errors.write(r, 401);
                        })
                    .accessDeniedHandler((q, r, x) -> errors.write(r, 403))
                    .jwt(
                        j ->
                            j.jwtAuthenticationConverter(
                                jwt -> {
                                  try {
                                    var user = users.active(UUID.fromString(jwt.getSubject()));
                                    Number sv = jwt.getClaim("sv");
                                    if (sv == null || sv.longValue() != user.securityVersion())
                                      throw AppException.unauthorized();
                                    return new UsernamePasswordAuthenticationToken(
                                        new Actor(user.id(), user.role()),
                                        null,
                                        List.of(
                                            new SimpleGrantedAuthority(
                                                "ROLE_" + user.role().toUpperCase(Locale.ROOT))));
                                  } catch (AppException | IllegalArgumentException ex) {
                                    throw new org.springframework.security.oauth2.server.resource
                                        .InvalidBearerTokenException(
                                        "The session is no longer valid");
                                  }
                                })));
    return http.build();
  }

  @Bean
  CorsConfigurationSource cors(
      @Value("${app.security.allowed-origins:http://localhost:3000}") String origins) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
    config.setExposedHeaders(List.of("X-Request-Id", "Location", "Retry-After"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
