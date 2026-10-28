package com.dki.meeting.identity.application.model;

import java.util.UUID;

public final class IdentityCommands {
  private IdentityCommands() {}

  public record Login(String email, String password) {
    @Override
    public String toString() {
      return "Login[redacted]";
    }
  }

  public record Refresh(String refreshToken) {
    @Override
    public String toString() {
      return "Refresh[redacted]";
    }
  }

  public record Create(String email, String displayName, String password, String role) {
    @Override
    public String toString() {
      return "Create[redacted]";
    }
  }

  public record Update(UUID id, String displayName, String role, boolean active, long version) {}

  public record PasswordChange(String currentPassword, String newPassword) {
    @Override
    public String toString() {
      return "PasswordChange[redacted]";
    }
  }

  public record Directory(String keyword, int page, int pageSize) {}

  public record Page(int page, int pageSize) {}

  public record Bootstrap(String email, String password) {
    @Override
    public String toString() {
      return "Bootstrap[redacted]";
    }
  }
}
