package com.dki.meeting.booking.domain;

import java.time.*;

public final class WeeklyRecurrenceStrategy implements RecurrenceStrategy {
  public String frequency() {
    return "weekly";
  }

  public TimeWindow next(TimeWindow current, int interval, ZoneId zone) {
    return new TimeWindow(
        current.startsAt().atZone(zone).plusWeeks(interval).toInstant(),
        current.endsAt().atZone(zone).plusWeeks(interval).toInstant());
  }
}
