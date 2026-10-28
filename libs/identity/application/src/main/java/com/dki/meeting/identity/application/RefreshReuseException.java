package com.dki.meeting.identity.application;

import com.dki.meeting.foundation.AppException;

public class RefreshReuseException extends AppException {
  public RefreshReuseException() {
    super(
        com.dki.meeting.foundation.FailureKind.UNAUTHENTICATED,
        "Authentication.RefreshReuse",
        "This refresh session is no longer valid");
  }
}
