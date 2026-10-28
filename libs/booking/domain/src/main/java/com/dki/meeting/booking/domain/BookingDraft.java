package com.dki.meeting.booking.domain;

import com.dki.meeting.foundation.AppException;
import java.util.*;

public record BookingDraft(
    UUID roomId,
    String title,
    String description,
    Recurrence recurrence,
    List<TimeWindow> windows,
    List<UUID> attendeeIds) {
  public BookingDraft {
    if (roomId == null || title == null || title.isBlank() || title.length() > 150)
      throw AppException.invalid("A room and valid meeting title are required");
    windows = List.copyOf(windows);
    attendeeIds = List.copyOf(attendeeIds);
    title = title.trim();
    if (windows.isEmpty() || windows.size() > 52)
      throw AppException.invalid("A booking must contain 1-52 occurrences");
    if (new HashSet<>(attendeeIds).size() != attendeeIds.size())
      throw AppException.invalid("Attendees must be unique");
  }
}
