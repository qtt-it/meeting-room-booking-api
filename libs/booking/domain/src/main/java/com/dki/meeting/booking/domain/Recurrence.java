package com.dki.meeting.booking.domain;

import com.dki.meeting.foundation.AppException;

public record Recurrence(String frequency, int interval, int count) {
  public Recurrence {
    if (!java.util.Set.of("daily", "weekly").contains(frequency)
        || interval < 1
        || interval > 12
        || count < 2
        || count > 52)
      throw AppException.invalid(
          "Recurrence requires daily or weekly, interval 1-12 and count 2-52");
  }
}
