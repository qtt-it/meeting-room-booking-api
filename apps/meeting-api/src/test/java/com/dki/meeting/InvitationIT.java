package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;

class InvitationIT extends ApiTestSupport {
  @Test
  void invitesSpecificEmployeesAndRestrictsChanges() throws Exception {
    var invited = user("invited@example.com");
    user("other@example.com");
    String invitedToken = login("invited@example.com", PASSWORD).path("accessToken").asText(),
        other = login("other@example.com", PASSWORD).path("accessToken").asText();
    var b =
        book(
            admin,
            room("Main", 10).path("id").asText(),
            start(),
            List.of(invited.path("id").asText()),
            null);
    String path = "/api/v1/bookings/" + b.path("id").asText();
    call("GET", path, invitedToken, null, 200);
    call("GET", path, other, null, 403);
    call("PUT", path + "/invitation-response", other, Map.of("response", "accepted"), 403);
    call("PUT", path + "/invitation-response", invitedToken, Map.of("response", "maybe"), 400);
    call("POST", path + "/approval", invitedToken, Map.of("version", 0), 403);
    call("POST", path + "/cancellation", invitedToken, Map.of("version", 0), 403);
    var accepted =
        call(
            "PUT",
            path + "/invitation-response",
            invitedToken,
            Map.of("response", "accepted"),
            200);
    assertThat(accepted.path("data").path("attendees").get(0).path("response").asText())
        .isEqualTo("accepted");
    var invitedList = call("GET", "/api/v1/bookings?scope=invited", invitedToken, null, 200);
    assertThat(invitedList.path("data").size()).isEqualTo(1);
    call("PUT", path + "/invitation-response", invitedToken, Map.of("response", "declined"), 200);
    call("POST", path + "/cancellation", admin, Map.of("version", 0), 200);
    call("PUT", path + "/invitation-response", other, Map.of("response", "accepted"), 403);
    call("PUT", path + "/invitation-response", invitedToken, Map.of("response", "accepted"), 422);
  }

  @Test
  void calendarHidesMeetingDetailsAndAvailabilityFiltersBusyRooms() throws Exception {
    user("viewer@example.com");
    String viewer = login("viewer@example.com", PASSWORD).path("accessToken").asText();
    String room = room("Main", 10).path("id").asText();
    approve(book(admin, room, start(), List.of(), null), 200);
    var calendar =
        call(
            "GET",
            "/api/v1/rooms/"
                + room
                + "/calendar?from="
                + start()
                + "&to="
                + start().plusSeconds(7200),
            viewer,
            null,
            200);
    assertThat(calendar.path("data").size()).isEqualTo(1);
    assertThat(calendar.path("data").get(0).has("title")).isFalse();
    assertThat(calendar.path("data").get(0).has("organizerId")).isFalse();
    assertThat(calendar.path("data").get(0).has("attendees")).isFalse();
    call(
        "GET",
        "/api/v1/rooms/"
            + room
            + "/calendar?from="
            + start()
            + "&to="
            + start().plus(Duration.ofDays(32)),
        viewer,
        null,
        422);
    assertThat(
            call(
                    "GET",
                    "/api/v1/rooms/availability?startsAt="
                        + start()
                        + "&endsAt="
                        + start().plusSeconds(3600)
                        + "&equipment=projector",
                    admin,
                    null,
                    200)
                .path("data")
                .size())
        .isZero();
  }

  @Test
  void availabilityCombinesMetadataFiltersAndPendingDoesNotReserve() throws Exception {
    String main = room("Main", 10).path("id").asText();
    room("Tiny", 1);
    call(
        "POST",
        "/api/v1/rooms",
        admin,
        Map.of(
            "name",
            "Remote",
            "capacity",
            20,
            "location",
            "Branch",
            "floor",
            "3",
            "equipment",
            List.of("projector")),
        201);
    var pending = book(admin, main, start(), List.of(), null);
    String query =
        "/api/v1/rooms/availability?startsAt="
            + start()
            + "&endsAt="
            + start().plusSeconds(3600)
            + "&minCapacity=2&location=HQ&floor=2&equipment=projector,whiteboard";
    var available = call("GET", query, admin, null, 200).path("data");
    assertThat(available.size()).isEqualTo(1);
    assertThat(available.get(0).path("id").asText()).isEqualTo(main);
    approve(pending, 200);
    assertThat(call("GET", query, admin, null, 200).path("data").size()).isZero();
  }

  @Test
  void suggestionsFitTheCapacityAndTheWholeRecurringSeries() throws Exception {
    String target = room("Main", 10).path("id").asText();
    String alternative = room("Alternative", 10).path("id").asText();
    String tiny = room("Tiny", 1).path("id").asText();
    var invited = user("invited@example.com");
    var series =
        book(
            admin,
            target,
            start(),
            List.of(invited.path("id").asText()),
            Map.of("frequency", "weekly", "interval", 1, "count", 2));
    approve(book(admin, target, start(), List.of(), null), 200);
    var details = approve(series, 409).path("error").path("details");
    boolean found = false;
    for (var suggestion : details.path("suggestions")) {
      assertThat(suggestion.path("occurrences").size()).isEqualTo(2);
      assertThat(suggestion.path("roomId").asText()).isNotEqualTo(tiny);
      if (suggestion.path("roomId").asText().equals(alternative)) found = true;
    }
    assertThat(found).isTrue();
    var unchanged =
        call("GET", "/api/v1/bookings/" + series.path("id").asText(), admin, null, 200)
            .path("data");
    assertThat(unchanged.path("status").asText()).isEqualTo("pending");
  }
}
