package com.dki.meeting.foundation;

import java.util.UUID;

public record Actor(UUID id, String role) {
  public boolean isAdmin() {
    return "admin".equals(role);
  }

  public void requireAdmin() {
    if (!isAdmin()) throw AppException.forbidden();
  }
}
