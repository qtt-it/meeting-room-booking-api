package com.dki.meeting.booking.application.service;

import com.dki.meeting.booking.application.model.BookingView;
import com.dki.meeting.booking.application.model.BookingView.*;
import com.dki.meeting.booking.domain.Booking;

public final class BookingMapper {
  private BookingMapper() {}

  public static BookingView toView(Booking b) {
    var r = b.recurrence();
    return new BookingView(
        b.id(),
        b.roomId(),
        b.organizerId(),
        b.title(),
        b.description(),
        b.status(),
        r == null ? null : new RecurrenceView(r.frequency(), r.interval(), r.count()),
        b.version(),
        b.createdAt(),
        b.updatedAt(),
        b.decidedBy(),
        b.decisionReason(),
        b.occurrences().stream()
            .map(o -> new OccurrenceView(o.id(), o.startsAt(), o.endsAt(), o.status()))
            .toList(),
        b.attendees().stream().map(a -> new AttendeeView(a.userId(), a.response())).toList());
  }
}
