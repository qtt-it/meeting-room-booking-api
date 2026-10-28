package com.dki.meeting.booking.application.service;

import com.dki.meeting.booking.application.model.RoomSummary;
import com.dki.meeting.foundation.AppException;
import java.util.*;
import java.util.function.Predicate;

public record RoomRequirements(int capacity, String location, String floor, List<String> equipment)
    implements Predicate<RoomSummary> {
  public RoomRequirements {
    if (capacity < 1 || capacity > 200)
      throw AppException.invalid("Capacity must be between 1 and 200");
    equipment = equipment.stream().map(s -> s.toLowerCase(Locale.ROOT)).distinct().toList();
  }

  public boolean test(RoomSummary room) {
    return room.capacity() >= capacity
        && (location.isBlank() || room.location().equalsIgnoreCase(location))
        && (floor.isBlank() || room.floor().equalsIgnoreCase(floor))
        && room.equipment().containsAll(equipment);
  }
}
