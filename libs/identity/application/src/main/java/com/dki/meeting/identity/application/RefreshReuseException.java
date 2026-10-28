package com.dki.meeting.identity.application;

import com.dki.meeting.foundation.AppException;

public class RefreshReuseException extends AppException {
  public RefreshReuseException() {
    super(401, "Authentication.RefreshReuse", "This refresh session is no longer valid");
  }
}
