package com.dki.meeting.booking.domain;

import java.time.*;

public final class DailyRecurrenceStrategy implements RecurrenceStrategy {
  public String frequency() {
    return "daily";
  }

  public TimeWindow next(TimeWindow current, int interval, ZoneId zone) {
    var start = current.startsAt().atZone(zone);
    var end = current.endsAt().atZone(zone);
    for (int n = 0; n < interval; n++) {
      do {
        start = start.plusDays(1);
        end = end.plusDays(1);
      } while (start.getDayOfWeek() == DayOfWeek.SUNDAY);
    }
    return new TimeWindow(start.toInstant(), end.toInstant());
  }
}
