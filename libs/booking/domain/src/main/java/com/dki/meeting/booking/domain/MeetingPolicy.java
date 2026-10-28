package com.dki.meeting.booking.domain;

import com.dki.meeting.foundation.AppException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class MeetingPolicy {
  public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  public List<TimeWindow> expand(TimeWindow first, Recurrence recurrence, Instant now) {
    int count = recurrence == null ? 1 : recurrence.count();
    var result = new ArrayList<TimeWindow>();
    TimeWindow next = first;
    for (int i = 0; i < count; i++) {
      validate(next, now, true);
      result.add(next);
      if (recurrence != null) {
        var start = next.startsAt().atZone(ZONE);
        var end = next.endsAt().atZone(ZONE);
        if (recurrence.frequency().equals("weekly")) {
          start = start.plusWeeks(recurrence.interval());
          end = end.plusWeeks(recurrence.interval());
        } else {
          for (int n = 0; n < recurrence.interval(); n++) {
            do {
              start = start.plusDays(1);
              end = end.plusDays(1);
            } while (start.getDayOfWeek() == DayOfWeek.SUNDAY);
          }
        }
        next = new TimeWindow(start.toInstant(), end.toInstant());
      }
    }
    return List.copyOf(result);
  }

  public void validate(TimeWindow window, Instant now, boolean future) {
    var start = window.startsAt().atZone(ZONE);
    var end = window.endsAt().atZone(ZONE);
    if (future && !window.startsAt().isAfter(now))
      throw AppException.invalid("The meeting must start in the future");
    if (window.endsAt().isAfter(now.plus(366, ChronoUnit.DAYS)))
      throw AppException.invalid("The booking horizon is 366 days");
    if (!start.toLocalDate().equals(end.toLocalDate())
        || start.getDayOfWeek() == DayOfWeek.SUNDAY
        || start.toLocalTime().isBefore(LocalTime.of(9, 0))
        || end.toLocalTime().isAfter(LocalTime.of(18, 0)))
      throw AppException.invalid("Meetings must be Monday-Saturday, 09:00-18:00 Asia/Ho_Chi_Minh");
    if (!window.startsAt().equals(window.startsAt().truncatedTo(ChronoUnit.MINUTES))
        || !window.endsAt().equals(window.endsAt().truncatedTo(ChronoUnit.MINUTES)))
      throw AppException.invalid("Meeting times must use whole minutes");
  }
}
