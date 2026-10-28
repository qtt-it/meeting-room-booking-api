package com.dki.meeting.booking.domain;

import java.time.ZoneId;

public interface RecurrenceStrategy {
  String frequency();

  TimeWindow next(TimeWindow current, int interval, ZoneId zone);
}
