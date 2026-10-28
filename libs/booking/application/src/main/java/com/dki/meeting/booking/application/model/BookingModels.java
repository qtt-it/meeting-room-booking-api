package com.dki.meeting.booking.application.model;

import com.dki.meeting.booking.domain.*;
import java.util.*;

public final class BookingModels {
  private BookingModels() {}

  public record Input(
      UUID roomId,
      String title,
      String description,
      TimeWindow first,
      Recurrence recurrence,
      List<UUID> attendeeIds) {}

  public record Suggestion(
      String type, UUID roomId, String roomName, List<TimeWindow> occurrences) {}
}
