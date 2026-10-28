package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;

class ApprovalIT extends ApiTestSupport {
  @Test
  void protectsDecisionPermissionsAndRequiresARejectionReason() throws Exception {
    user("owner@example.com");
    user("outsider@example.com");
    String owner = login("owner@example.com", PASSWORD).path("accessToken").asText();
    String outsider = login("outsider@example.com", PASSWORD).path("accessToken").asText();
    String room = room("Main", 10).path("id").asText();
    var b = book(owner, room, start(), List.of(), null);
    String path = "/api/v1/bookings/" + b.path("id").asText();
    call("POST", path + "/approval", owner, Map.of("version", 0), 403);
    call("POST", path + "/rejection", owner, Map.of("version", 0, "reason", "No"), 403);
    call("POST", path + "/cancellation", outsider, Map.of("version", 0), 403);
    call("POST", path + "/rejection", admin, Map.of("version", 0), 422);
    var rejected =
        call("POST", path + "/rejection", admin, Map.of("version", 0, "reason", "Maintenance"), 200)
            .path("data");
    assertThat(rejected.path("status").asText()).isEqualTo("rejected");
    call("POST", path + "/approval", admin, Map.of("version", 1), 422);
  }

  @Test
  void cancellationPreservesPastAndOngoingOccurrences() throws Exception {
    String room = room("Main", 10).path("id").asText();
    var b =
        approve(
                book(
                    admin,
                    room,
                    start(),
                    List.of(),
                    Map.of("frequency", "weekly", "interval", 1, "count", 3)),
                200)
            .path("data");
    Instant now = Instant.now();
    for (int i = 0; i < 2; i++) {
      Instant from = i == 0 ? now.minus(Duration.ofDays(3)) : now.minusSeconds(1800);
      jdbc.update(
          "update booking_occurrence set starts_at=?, ends_at=? where id=?",
          java.sql.Timestamp.from(from),
          java.sql.Timestamp.from(from.plusSeconds(3600)),
          UUID.fromString(b.path("occurrences").get(i).path("id").asText()));
    }
    var result =
        call(
                "POST",
                "/api/v1/bookings/" + b.path("id").asText() + "/cancellation",
                admin,
                Map.of("version", b.path("version").asLong()),
                200)
            .path("data");
    assertThat(result.path("status").asText()).isEqualTo("cancelled");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from booking_occurrence where status='approved'", Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from booking_occurrence where status='cancelled'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void firstApprovalWinsAndReturnsSuggestions() throws Exception {
    String id = room("Main", 10).path("id").asText();
    room("Alternative", 10);
    var a = book(admin, id, start(), List.of(), null);
    var b = book(admin, id, start(), List.of(), null);
    approve(a, 200);
    var conflict = approve(b, 409);
    assertThat(conflict.path("error").path("code").asText()).isEqualTo("Booking.ScheduleConflict");
    assertThat(conflict.path("error").path("details").path("suggestions").size()).isGreaterThan(0);
    call("POST", "/api/v1/bookings", admin, bookingBody(id, start(), List.of(), null), 409);
  }

  @Test
  void simultaneousApprovalsCannotDoubleBook() throws Exception {
    String id = room("Main", 10).path("id").asText();
    var a = book(admin, id, start(), List.of(), null);
    var b = book(admin, id, start(), List.of(), null);
    var gate = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var tasks =
          List.of(a, b).stream()
              .map(
                  booking ->
                      pool.submit(
                          () -> {
                            gate.await();
                            return mvc.perform(
                                    post("/api/v1/bookings/"
                                            + booking.path("id").asText()
                                            + "/approval")
                                        .header("Authorization", "Bearer " + admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"version\":0}"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                          }))
              .toList();
      gate.countDown();
      var results = new ArrayList<Integer>();
      for (var t : tasks) results.add(t.get(20, TimeUnit.SECONDS));
      assertThat(results).containsExactlyInAnyOrder(200, 409);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from booking_occurrence where status='approved'", Long.class))
          .isEqualTo(1L);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void adjacentMeetingsAndDifferentRoomsAreAllowed() throws Exception {
    String one = room("One", 10).path("id").asText(), two = room("Two", 10).path("id").asText();
    approve(book(admin, one, start(), List.of(), null), 200);
    approve(book(admin, one, start().plusSeconds(3600), List.of(), null), 200);
    approve(book(admin, two, start(), List.of(), null), 200);
  }

  @Test
  void recurringApprovalIsAtomic() throws Exception {
    String room = room("Main", 10).path("id").asText();
    var recurring =
        book(
            admin,
            room,
            start(),
            List.of(),
            Map.of("frequency", "weekly", "interval", 1, "count", 3));
    approve(book(admin, room, start().plus(Duration.ofDays(7)), List.of(), null), 200);
    approve(recurring, 409);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from booking_occurrence where booking_id=? and status='approved'",
                Long.class,
                UUID.fromString(recurring.path("id").asText())))
        .isZero();
  }

  @Test
  void staleVersionFailsAndEditingRequiresApprovalAgain() throws Exception {
    String room = room("Main", 10).path("id").asText();
    var b = approve(book(admin, room, start(), List.of(), null), 200).path("data");
    String path = "/api/v1/bookings/" + b.path("id").asText();
    call("POST", path + "/cancellation", admin, Map.of("version", 0), 409);
    var updated = bookingBody(room, start().plusSeconds(3600), List.of(), null);
    updated.put("version", b.path("version").asLong());
    assertThat(call("PUT", path, admin, updated, 200).path("data").path("status").asText())
        .isEqualTo("pending");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from booking_occurrence where status='approved'", Long.class))
        .isZero();
  }

  @Test
  void cancellationReleasesFutureReservationsAndRoomCanBeDeactivated() throws Exception {
    var room = room("Main", 10);
    var b =
        approve(book(admin, room.path("id").asText(), start(), List.of(), null), 200).path("data");
    var body =
        Map.of(
            "name",
            "Main",
            "location",
            "HQ",
            "floor",
            "2",
            "capacity",
            10,
            "equipment",
            List.of("projector"),
            "isActive",
            false,
            "version",
            0);
    call("PUT", "/api/v1/rooms/" + room.path("id").asText(), admin, body, 409);
    call(
        "POST",
        "/api/v1/bookings/" + b.path("id").asText() + "/cancellation",
        admin,
        Map.of("version", b.path("version").asLong()),
        200);
    call("PUT", "/api/v1/rooms/" + room.path("id").asText(), admin, body, 200);
  }

  @Test
  void databaseConstraintRejectsDirectOverlap() throws Exception {
    String room = room("Main", 10).path("id").asText();
    var one = book(admin, room, start(), List.of(), null);
    var two = book(admin, room, start(), List.of(), null);
    approve(one, 200);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update booking_occurrence set status='approved' where booking_id=?",
                    UUID.fromString(two.path("id").asText())))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }
}
