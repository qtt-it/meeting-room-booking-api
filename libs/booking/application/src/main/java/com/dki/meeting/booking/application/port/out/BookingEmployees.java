package com.dki.meeting.booking.application.port.out;

import java.util.*;

public interface BookingEmployees {
  void requireActive(UUID id);

  void validateInvitees(List<UUID> ids);
}
