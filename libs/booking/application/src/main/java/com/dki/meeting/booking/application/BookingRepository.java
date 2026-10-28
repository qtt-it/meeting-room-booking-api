package com.dki.meeting.booking.application;

import com.dki.meeting.booking.domain.*;
import com.dki.meeting.foundation.*;
import java.time.Instant;
import java.util.*;

public interface BookingRepository {
  Optional<Booking> byId(UUID id, boolean lock);

  void insert(Booking booking, UUID actor);

  void replace(Booking booking, UUID actor);

  void changeStatus(UUID id, String status, UUID actor, String reason, Instant now);

  PageResult<Booking> list(Actor actor, String scope, String status, int page, int size);

  List<TimeWindow> occupied(UUID roomId, Instant from, Instant to, UUID exclude);

  void audit(UUID bookingId, UUID actor, String action, String details);
}
