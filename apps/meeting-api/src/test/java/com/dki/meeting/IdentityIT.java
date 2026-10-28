package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;

class IdentityIT extends ApiTestSupport {
  @org.junit.jupiter.api.Test
  void openApiMarksLoginAsPublic() throws Exception {
    var api = call("GET", "/v3/api-docs", null, null, 200);
    assertThat(api.path("paths").path("/api/v1/auth/login").path("post").path("security").isArray())
        .isTrue();
    assertThat(api.path("paths").path("/api/v1/auth/login").path("post").path("security").size())
        .isZero();
  }

  @org.junit.jupiter.api.Test
  void concurrentRefreshReuseCannotLeaveAUsableDescendant() throws Exception {
    var first = login("admin@example.com", PASSWORD);
    String old = first.path("refreshToken").asText();
    String current =
        call("POST", "/api/v1/auth/refresh", null, java.util.Map.of("refreshToken", old), 200)
            .path("data")
            .path("refreshToken")
            .asText();
    var gate = new java.util.concurrent.CountDownLatch(1);
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var futures =
          java.util.List.of(old, current).stream()
              .map(
                  raw ->
                      pool.submit(
                          () -> {
                            gate.await();
                            return mvc.perform(
                                    org.springframework.test.web.servlet.request
                                        .MockMvcRequestBuilders.post("/api/v1/auth/refresh")
                                        .contentType(
                                            org.springframework.http.MediaType.APPLICATION_JSON)
                                        .content(
                                            json.writeValueAsString(
                                                java.util.Map.of("refreshToken", raw))))
                                .andReturn();
                          }))
              .toList();
      gate.countDown();
      for (var future : futures) {
        var response = future.get(20, java.util.concurrent.TimeUnit.SECONDS).getResponse();
        assertThat(response.getStatus()).isIn(200, 401);
        if (response.getStatus() == 200) {
          String descendant =
              json.readTree(response.getContentAsString())
                  .path("data")
                  .path("refreshToken")
                  .asText();
          call(
              "POST",
              "/api/v1/auth/refresh",
              null,
              java.util.Map.of("refreshToken", descendant),
              401);
        }
      }
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from refresh_family where revoked_at is not null", Long.class))
          .isGreaterThan(0);
    } finally {
      pool.shutdownNow();
    }
  }

  @org.junit.jupiter.api.Test
  void accountAndSessionMutationsRecordAuditWithoutCredentials() throws Exception {
    var created = user("audit@example.com");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from activity_audit where resource_id=? and action='user_created'",
                Long.class,
                java.util.UUID.fromString(created.path("id").asText())))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from activity_audit where action='session_created'", Long.class))
        .isGreaterThan(0);
  }

  @Test
  void enforcesRoleAndHidesPassword() throws Exception {
    user("employee@example.com");
    String token = login("employee@example.com", PASSWORD).path("accessToken").asText();
    assertThat(call("GET", "/api/v1/users/me", token, null, 200).path("data").has("passwordHash"))
        .isFalse();
    call("GET", "/api/v1/users", token, null, 403);
    call("GET", "/api/v1/rooms", null, null, 401);
    call(
        "POST",
        "/api/v1/auth/login",
        null,
        Map.of("email", "employee@example.com", "password", "invalid"),
        401);
  }

  @Test
  void rotatesRefreshAndRevokesFamilyOnReuse() throws Exception {
    var auth = login("admin@example.com", PASSWORD);
    String old = auth.path("refreshToken").asText();
    var rotated =
        call("POST", "/api/v1/auth/refresh", null, Map.of("refreshToken", old), 200).path("data");
    call("POST", "/api/v1/auth/refresh", null, Map.of("refreshToken", old), 401);
    call(
        "POST",
        "/api/v1/auth/refresh",
        null,
        Map.of("refreshToken", rotated.path("refreshToken").asText()),
        401);
  }

  @Test
  void logoutRevokesRefresh() throws Exception {
    var auth = login("admin@example.com", PASSWORD);
    var body = Map.of("refreshToken", auth.path("refreshToken").asText());
    call("POST", "/api/v1/auth/logout", null, body, 200);
    call("POST", "/api/v1/auth/refresh", null, body, 401);
  }

  @Test
  void passwordChangeInvalidatesAccessAndRefresh() throws Exception {
    var auth = login("admin@example.com", PASSWORD);
    call(
        "PUT",
        "/api/v1/users/me/password",
        auth.path("accessToken").asText(),
        Map.of("currentPassword", PASSWORD, "newPassword", "Replacement!2026Pass"),
        200);
    call("GET", "/api/v1/users/me", auth.path("accessToken").asText(), null, 401);
    call(
        "POST",
        "/api/v1/auth/refresh",
        null,
        Map.of("refreshToken", auth.path("refreshToken").asText()),
        401);
    login("admin@example.com", "Replacement!2026Pass");
  }
}
