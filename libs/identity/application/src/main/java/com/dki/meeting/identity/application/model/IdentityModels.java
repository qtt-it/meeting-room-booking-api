package com.dki.meeting.identity.application.model;

import com.dki.meeting.identity.domain.UserAccount;
import java.time.Instant;
import java.util.UUID;

public final class IdentityModels {
  private IdentityModels() {}

  public record UserView(
      UUID id,
      String email,
      String displayName,
      String role,
      boolean isActive,
      long version,
      Instant createdAt) {
    public static UserView from(UserAccount u) {
      return new UserView(
          u.id(), u.email(), u.displayName(), u.role(), u.isActive(), u.version(), u.createdAt());
    }
  }

  public record Tokens(
      String accessToken, String refreshToken, String tokenType, long expiresIn, UserView user) {
    @Override
    public String toString() {
      return "Tokens[redacted]";
    }
  }
}
