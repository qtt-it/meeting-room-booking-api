package com.dki.meeting.booking.application.model;

import com.dki.meeting.booking.domain.TimeWindow;
import java.time.Instant;
import java.util.*;

public final class BookingCommands {
  private BookingCommands() {}

  public record Update(UUID id, BookingModels.Input input, long version) {}

  public record Decision(UUID id, long version, String reason) {}

  public record Respond(UUID id, String response) {}

  public record Page(String scope, String status, int page, int pageSize) {}

  public record Calendar(UUID roomId, Instant from, Instant to, int page, int pageSize) {}

  public record Availability(
      TimeWindow window, int minCapacity, String location, String floor, List<String> equipment) {}
}
