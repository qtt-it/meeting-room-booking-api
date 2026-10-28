package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;

class RoomIT extends ApiTestSupport {
  @Test
  void enforcesRolesVersionsAndPublicRoomShape() throws Exception {
    var u = user("staff@example.com");
    String token = login("staff@example.com", PASSWORD).path("accessToken").asText();
    var r = room("Boardroom", 10);
    assertThat(r.has("slot")).isFalse();
    var input = new HashMap<String, Object>();
    input.put("name", "Boardroom");
    input.put("location", "HQ");
    input.put("floor", "2");
    input.put("capacity", 10);
    input.put("equipment", List.of(" Projector ", "projector"));
    call("POST", "/api/v1/rooms", token, input, 403);
    call("POST", "/api/v1/rooms", admin, input, 409);
    input.put("isActive", false);
    input.put("version", 0);
    call("PUT", "/api/v1/rooms/" + r.path("id").asText(), admin, input, 200);
    call("PUT", "/api/v1/rooms/" + r.path("id").asText(), admin, input, 409);
    call("GET", "/api/v1/rooms/" + r.path("id").asText(), token, null, 403);
    call("GET", "/api/v1/rooms?includeInactive=true", token, null, 403);
    var result = call("GET", "/api/v1/rooms?includeInactive=true&pageSize=1", admin, null, 200);
    assertThat(result.path("meta").path("pagination").path("totalItems").asInt()).isEqualTo(1);
    input.put("isActive", true);
    input.put("version", 1);
    var activated =
        call("PUT", "/api/v1/rooms/" + r.path("id").asText(), admin, input, 200).path("data");
    assertThat(activated.path("equipment").size()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from activity_audit where resource_type='room'", Integer.class))
        .isEqualTo(3);
    call("GET", "/api/v1/rooms?page=0", admin, null, 400);
  }

  @Test
  void serializesConcurrentCreationAtTheLimit() throws Exception {
    for (int i = 1; i <= 9; i++) room("Room " + i, 10);
    var executor = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    try {
      var futures = new ArrayList<Future<Integer>>();
      for (int i = 10; i <= 11; i++) {
        final int index = i;
        futures.add(
            executor.submit(
                () -> {
                  gate.await();
                  return mvc.perform(
                          post("/api/v1/rooms")
                              .header("Authorization", "Bearer " + admin)
                              .contentType("application/json")
                              .content(
                                  json.writeValueAsBytes(
                                      Map.of(
                                          "name",
                                          "Room " + index,
                                          "location",
                                          "HQ",
                                          "floor",
                                          "2",
                                          "capacity",
                                          10,
                                          "equipment",
                                          List.of()))))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      }
      gate.countDown();
      assertThat(
              List.of(
                  futures.get(0).get(20, TimeUnit.SECONDS),
                  futures.get(1).get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
      assertThat(jdbc.queryForObject("select count(*) from room where is_active", Integer.class))
          .isEqualTo(10);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void limitsActiveRoomsToTen() throws Exception {
    for (int i = 1; i <= 10; i++) room("Room " + i, 10);
    call(
        "POST",
        "/api/v1/rooms",
        admin,
        Map.of(
            "name",
            "Room 11",
            "location",
            "HQ",
            "floor",
            "2",
            "capacity",
            10,
            "equipment",
            List.of()),
        409);
  }
}
