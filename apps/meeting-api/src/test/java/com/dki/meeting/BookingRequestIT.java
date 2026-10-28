package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class BookingRequestIT extends ApiTestSupport {
  @Test
  void protectsDetailsEditsAndAllScopeAndLoadsPagedChildren() throws Exception {
    user("outsider@example.com");
    String outsider = login("outsider@example.com", PASSWORD).path("accessToken").asText();
    var invitee = user("invitee@example.com");
    String room = room("Private", 10).path("id").asText();
    var booking = book(admin, room, start(), List.of(invitee.path("id").asText()), null);
    String path = "/api/v1/bookings/" + booking.path("id").asText();
    call("GET", path, outsider, null, 403);
    var update = bookingBody(room, start(), List.of(), null);
    update.put("version", 0);
    call("PUT", path, outsider, update, 403);
    call("GET", "/api/v1/bookings?scope=all", outsider, null, 403);
    call("GET", "/api/v1/bookings?status=unknown", admin, null, 400);
    var page = call("GET", "/api/v1/bookings?pageSize=1", admin, null, 200);
    assertThat(page.path("data").get(0).path("occurrences").size()).isEqualTo(1);
    assertThat(page.path("data").get(0).path("attendees").get(0).path("userId").asText())
        .isEqualTo(invitee.path("id").asText());
    assertThat(page.path("meta").path("pagination").path("totalItems").asInt()).isEqualTo(1);
  }

  @Test
  void employeeSubmitsARecurringRequestAndPendingDoesNotHoldTheRoom() throws Exception {
    user("owner@example.com");
    String owner = login("owner@example.com", PASSWORD).path("accessToken").asText();
    String room = room("Main", 10).path("id").asText();
    var booking =
        book(
            owner,
            room,
            start(),
            List.of(),
            Map.of("frequency", "weekly", "interval", 1, "count", 4));
    assertThat(booking.path("status").asText()).isEqualTo("pending");
    assertThat(booking.path("occurrences").size()).isEqualTo(4);
    book(owner, room, start(), List.of(), null);
    String path = "/api/v1/bookings/" + booking.path("id").asText();
    var update = bookingBody(room, start().plusSeconds(3600), List.of(), null);
    update.put("version", 0);
    assertThat(call("PUT", path, owner, update, 200).path("data").path("version").asLong())
        .isEqualTo(1);
  }
}
