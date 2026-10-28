package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;

class BookingIT extends ApiTestSupport {
  @Test
  void rejectsInvalidCapacityHoursAndSelfInvitation() throws Exception {
    String room = room("Small", 1).path("id").asText();
    var invited = user("invited@example.com");
    call(
        "POST",
        "/api/v1/bookings",
        admin,
        bookingBody(room, start(), List.of(invited.path("id").asText()), null),
        422);
    call(
        "POST",
        "/api/v1/bookings",
        admin,
        bookingBody(room, start().minusSeconds(7200), List.of(), null),
        422);
    String me = call("GET", "/api/v1/users/me", admin, null, 200).path("data").path("id").asText();
    call("POST", "/api/v1/bookings", admin, bookingBody(room, start(), List.of(me), null), 422);
  }

  @Test
  void lastAdminCannotBeDisabledAndInvalidInputsAreBadRequests() throws Exception {
    var me = call("GET", "/api/v1/users/me", admin, null, 200).path("data");
    call(
        "PUT",
        "/api/v1/users/" + me.path("id").asText(),
        admin,
        Map.of("displayName", "Admin", "role", "admin", "isActive", false, "version", 0),
        422);
    call("GET", "/api/v1/bookings?page=0", admin, null, 400);
    call("POST", "/api/v1/bookings", admin, Map.of("title", "missing fields"), 400);
  }
}
