package com.dki.meeting.room.application.model;

import java.util.*;

public final class RoomCommands {
  private RoomCommands() {}

  public record Input(
      String name,
      String description,
      String location,
      String floor,
      int capacity,
      List<String> equipment) {}

  public record Catalog(boolean includeInactive, int page, int pageSize) {}

  public record Update(UUID id, Input input, boolean active, long version) {}
}
