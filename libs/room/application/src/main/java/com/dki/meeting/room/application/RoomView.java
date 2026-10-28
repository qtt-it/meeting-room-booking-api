package com.dki.meeting.room.application;

import com.dki.meeting.room.domain.Room;
import java.time.Instant;
import java.util.*;

public record RoomView(
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
  public static RoomView from(Room r) {
    return new RoomView(
        r.id(),
        r.name(),
        r.description(),
        r.location(),
        r.floor(),
        r.capacity(),
        r.equipment(),
        r.isActive(),
        r.version(),
        r.createdAt());
  }
}
