package com.dki.meeting.booking.application.model;

import java.time.Instant;
import java.util.*;

public record RoomSummary(
    UUID id,
    String name,
    String description,
    String location,
    String floor,
    int capacity,
    List<String> equipment,
    boolean isActive,
    long version,
    Instant createdAt) {
  public RoomSummary {
    equipment = List.copyOf(equipment);
  }
}
