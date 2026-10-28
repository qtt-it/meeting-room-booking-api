package com.dki.meeting.booking.application.model;

import java.time.Instant;
import java.util.*;

public record BookingView(
    UUID id,
    UUID roomId,
    UUID organizerId,
    String title,
    String description,
    String status,
    RecurrenceView recurrence,
    long version,
    Instant createdAt,
    Instant updatedAt,
    UUID decidedBy,
    String decisionReason,
    List<OccurrenceView> occurrences,
    List<AttendeeView> attendees) {
  public BookingView {
    occurrences = List.copyOf(occurrences);
    attendees = List.copyOf(attendees);
  }

  public record RecurrenceView(String frequency, int interval, int count) {}

  public record OccurrenceView(UUID id, Instant startsAt, Instant endsAt, String status) {}

  public record AttendeeView(UUID userId, String response) {}
}
