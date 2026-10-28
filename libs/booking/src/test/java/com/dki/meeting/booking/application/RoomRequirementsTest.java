package com.dki.meeting.booking.application;

import static org.assertj.core.api.Assertions.*;

import com.dki.meeting.booking.application.model.RoomSummary;
import com.dki.meeting.booking.application.service.RoomRequirements;
import com.dki.meeting.foundation.AppException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class RoomRequirementsTest {
  private RoomSummary room(int capacity, String location, String floor, List<String> equipment) {
    return new RoomSummary(
        UUID.randomUUID(),
        "Room",
        null,
        location,
        floor,
        capacity,
        equipment,
        true,
        0,
        Instant.EPOCH);
  }

  @Test
  void combinesCapacityLocationFloorAndAllEquipment() {
    var spec = new RoomRequirements(5, "HQ", "2", List.of("PROJECTOR", "whiteboard"));
    assertThat(spec.test(room(5, "hq", "2", List.of("projector", "whiteboard")))).isTrue();
    assertThat(spec.test(room(4, "hq", "2", List.of("projector", "whiteboard")))).isFalse();
    assertThat(spec.test(room(5, "branch", "2", List.of("projector", "whiteboard")))).isFalse();
    assertThat(spec.test(room(5, "hq", "3", List.of("projector", "whiteboard")))).isFalse();
    assertThat(spec.test(room(5, "hq", "2", List.of("projector")))).isFalse();
  }

  @Test
  void optionalMetadataStillRequiresCapacity() {
    var spec = new RoomRequirements(2, "", "", List.of());
    assertThat(spec.test(room(2, "Branch", "4", List.of()))).isTrue();
    assertThat(spec.test(room(1, "Branch", "4", List.of()))).isFalse();
    assertThatThrownBy(() -> new RoomRequirements(0, "", "", List.of()))
        .isInstanceOf(AppException.class);
  }
}
