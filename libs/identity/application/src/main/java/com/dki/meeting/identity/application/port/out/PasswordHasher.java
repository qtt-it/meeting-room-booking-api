package com.dki.meeting.identity.application.port.out;

public interface PasswordHasher {
  String hash(String password);

  boolean matches(String password, String hash);
}
