package com.dki.meeting.booking.domain;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public final class BookingFactory {
  private BookingFactory() {}

  public static Booking create(
      UUID id, UUID organizer, BookingDraft draft, Instant now, Supplier<UUID> ids) {
    return pending(id, organizer, draft, 0, now, now, ids);
  }

  public static Booking reschedule(
      Booking current, BookingDraft draft, Instant now, Supplier<UUID> ids) {
    current.requireEditable(now);
    return pending(
        current.id(),
        current.organizerId(),
        draft,
        current.version() + 1,
        current.createdAt(),
        now,
        ids);
  }

  private static Booking pending(
      UUID id,
      UUID organizer,
      BookingDraft draft,
      long version,
      Instant created,
      Instant updated,
      Supplier<UUID> ids) {
    return new Booking(
        id,
        draft.roomId(),
        organizer,
        draft.title(),
        draft.description(),
        "pending",
        draft.recurrence(),
        version,
        created,
        updated,
        null,
        null,
        draft.windows().stream()
            .map(w -> new Booking.Occurrence(ids.get(), w.startsAt(), w.endsAt(), "pending"))
            .toList(),
        draft.attendeeIds().stream().map(user -> new Booking.Attendee(user, "pending")).toList());
  }
}
