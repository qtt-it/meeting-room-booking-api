package com.dki.meeting.identity.application.port.out;

import com.dki.meeting.identity.domain.UserAccount;

public interface AccessTokens {
  String issue(UserAccount user);

  long expiresInSeconds();
}
