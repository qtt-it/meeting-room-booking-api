package com.dki.meeting.booking.application.port.out;

import com.dki.meeting.booking.application.model.RoomSummary;
import java.util.*;

public interface BookingRooms {
  RoomSummary lockActive(UUID id);

  RoomSummary find(UUID id);

  List<RoomSummary> activeRooms();
}
