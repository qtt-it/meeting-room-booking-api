package com.dki.meeting.booking.domain;

import java.time.Instant;
import java.util.*;

public record Booking(
    UUID id,
    UUID roomId,
    UUID organizerId,
    String title,
    String description,
    String status,
    Recurrence recurrence,
    long version,
    Instant createdAt,
    Instant updatedAt,
    UUID decidedBy,
    String decisionReason,
    List<Occurrence> occurrences,
    List<Attendee> attendees) {
  public record Occurrence(UUID id, Instant startsAt, Instant endsAt, String status) {
    public TimeWindow window() {
      return new TimeWindow(startsAt, endsAt);
    }
  }

  public record Attendee(UUID userId, String response) {}

  public List<TimeWindow> windows() {
    return occurrences.stream().map(Occurrence::window).toList();
  }
}
