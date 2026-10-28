package com.dki.meeting.room.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface RoomUsage {
  boolean hasFutureReservations(UUID roomId, Instant now);

  int largestFutureMeeting(UUID roomId, Instant now);
}
