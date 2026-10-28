package com.dki.meeting.booking.application.service;

import com.dki.meeting.booking.domain.Booking;
import com.dki.meeting.foundation.*;

public final class BookingAuthorizer {
  public void requireManager(Actor actor, Booking booking) {
    if (!actor.isAdmin() && !booking.organizerId().equals(actor.id()))
      throw AppException.forbidden();
  }

  public void requireViewer(Actor actor, Booking booking) {
    if (!actor.isAdmin()
        && !booking.organizerId().equals(actor.id())
        && booking.attendees().stream().noneMatch(a -> a.userId().equals(actor.id())))
      throw AppException.forbidden();
  }
}
