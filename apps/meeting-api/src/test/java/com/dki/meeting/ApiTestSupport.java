package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.port.in.IdentityHandlers;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=${TEST_DATABASE_URL:jdbc:postgresql://localhost:55432/meeting_booking_test}",
      "spring.datasource.username=${TEST_DATABASE_USERNAME:meeting_test}",
      "spring.datasource.password=${TEST_DATABASE_PASSWORD:test_database_password}",
      "app.security.jwt-secret=test-only-signing-key-with-at-least-32-bytes",
      "app.bootstrap.email=",
      "app.bootstrap.password="
    })
abstract class ApiTestSupport {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired IdentityHandlers identity;
  protected String admin;
  protected static final String PASSWORD = "LocalTest!2026Pass";

  @BeforeEach
  void reset() throws Exception {
    String database = jdbc.queryForObject("select current_database()", String.class);
    if (!"meeting_booking_test".equals(database))
      throw new IllegalStateException(
          "Integration tests require isolated database meeting_booking_test");
    var tables =
        jdbc.queryForList(
            "select tablename from pg_tables where schemaname='public' and tablename in ('activity_audit','booking_audit','booking_attendee','booking_occurrence','booking','room','refresh_session','refresh_family','user_account')",
            String.class);
    jdbc.execute("truncate " + String.join(",", tables) + " cascade");
    identity
        .bootstrap()
        .execute(
            OperationContext.system(),
            new IdentityCommands.Bootstrap("admin@example.com", PASSWORD));
    admin = login("admin@example.com", PASSWORD).path("accessToken").asText();
  }

  protected JsonNode call(String method, String path, String token, Object body, int status)
      throws Exception {
    var r =
        request(org.springframework.http.HttpMethod.valueOf(method), path)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .with(
                req -> {
                  req.setRemoteAddr(UUID.randomUUID().toString());
                  return req;
                });
    if (token != null) r.header("Authorization", "Bearer " + token);
    if (body != null) r.content(json.writeValueAsBytes(body));
    var result = mvc.perform(r).andReturn();
    assertThat(result.getResponse().getStatus())
        .as(result.getResponse().getContentAsString())
        .isEqualTo(status);
    return json.readTree(result.getResponse().getContentAsString());
  }

  protected JsonNode login(String email, String password) throws Exception {
    return call(
            "POST", "/api/v1/auth/login", null, Map.of("email", email, "password", password), 200)
        .path("data");
  }

  protected JsonNode user(String email) throws Exception {
    return call(
            "POST",
            "/api/v1/users",
            admin,
            Map.of("email", email, "displayName", email, "password", PASSWORD, "role", "user"),
            201)
        .path("data");
  }

  protected JsonNode room(String name, int capacity) throws Exception {
    return call(
            "POST",
            "/api/v1/rooms",
            admin,
            Map.of(
                "name",
                name,
                "location",
                "HQ",
                "floor",
                "2",
                "capacity",
                capacity,
                "equipment",
                List.of("projector", "whiteboard")),
            201)
        .path("data");
  }

  protected Instant start() {
    var day = LocalDate.now(MeetingZone.ZONE).plusDays(2);
    while (day.getDayOfWeek() == DayOfWeek.SUNDAY) day = day.plusDays(1);
    return day.atTime(10, 0).atZone(MeetingZone.ZONE).toInstant();
  }

  protected Map<String, Object> bookingBody(
      String room, Instant start, List<String> attendees, Map<String, Object> recurrence) {
    var b = new HashMap<String, Object>();
    b.put("roomId", room);
    b.put("title", "Planning");
    b.put("startsAt", start.toString());
    b.put("endsAt", start.plusSeconds(3600).toString());
    b.put("attendeeIds", attendees);
    if (recurrence != null) b.put("recurrence", recurrence);
    return b;
  }

  protected JsonNode book(
      String token,
      String room,
      Instant start,
      List<String> attendees,
      Map<String, Object> recurrence)
      throws Exception {
    return call(
            "POST", "/api/v1/bookings", token, bookingBody(room, start, attendees, recurrence), 201)
        .path("data");
  }

  protected JsonNode approve(JsonNode b, int status) throws Exception {
    return call(
        "POST",
        "/api/v1/bookings/" + b.path("id").asText() + "/approval",
        admin,
        Map.of("version", b.path("version").asLong()),
        status);
  }

  static class MeetingZone {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  }
}
