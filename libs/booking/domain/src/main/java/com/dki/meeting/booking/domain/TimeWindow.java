package com.dki.meeting.booking.domain;

import com.dki.meeting.foundation.AppException;
import java.time.*;

public record TimeWindow(Instant startsAt, Instant endsAt) {
  public TimeWindow {
    if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt))
      throw AppException.invalid("The end time must be after the start time");
  }

  public boolean overlaps(TimeWindow other) {
    return startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt);
  }

  public TimeWindow shift(Duration shift) {
    return new TimeWindow(startsAt.plus(shift), endsAt.plus(shift));
  }
}
