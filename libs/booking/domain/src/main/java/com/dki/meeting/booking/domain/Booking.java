package com.dki.meeting.booking.domain;

import com.dki.meeting.foundation.AppException;
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
  public Booking {
    occurrences = List.copyOf(occurrences);
    attendees = List.copyOf(attendees);
  }

  public void requireVersion(long expected) {
    if (version != expected)
      throw AppException.conflict(
          "Concurrency.StaleVersion", "The booking has changed. Reload and retry");
  }

  public void requirePending() {
    if (!status.equals("pending"))
      throw AppException.invalid("Only pending bookings can be reviewed");
  }

  public void requireEditable(Instant now) {
    if (!Set.of("pending", "approved").contains(status)
        || windows().stream().anyMatch(w -> !w.startsAt().isAfter(now)))
      throw AppException.invalid("Only unstarted pending or approved bookings can be edited");
  }

  public void requireCancelable(Instant now) {
    if (!Set.of("pending", "approved").contains(status))
      throw AppException.invalid("Only pending or approved bookings can be canceled");
    if (windows().stream().noneMatch(w -> w.startsAt().isAfter(now)))
      throw AppException.invalid("This booking has no future occurrence to cancel");
  }

  public void requireAcceptingResponses(Instant now) {
    if (!Set.of("pending", "approved").contains(status))
      throw AppException.invalid("The booking is no longer open");
    if (windows().stream().noneMatch(w -> w.endsAt().isAfter(now)))
      throw AppException.invalid("The booking has ended");
  }

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
