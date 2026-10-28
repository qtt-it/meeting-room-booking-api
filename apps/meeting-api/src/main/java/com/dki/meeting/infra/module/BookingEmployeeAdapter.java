package com.dki.meeting.infra.module;

import com.dki.meeting.booking.application.port.out.BookingEmployees;
import com.dki.meeting.identity.application.service.IdentityService;
import java.util.*;

public final class BookingEmployeeAdapter implements BookingEmployees {
  private final IdentityService users;

  public BookingEmployeeAdapter(IdentityService users) {
    this.users = users;
  }

  public void requireActive(UUID id) {
    users.active(id);
  }

  public void validateInvitees(List<UUID> ids) {
    users.validateInvitees(ids);
  }
}
