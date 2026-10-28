package com.dki.meeting.foundation;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityActor {
  private SecurityActor() {}

  public static Actor current() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Actor actor))
      throw AppException.unauthorized();
    return actor;
  }
}
