package com.dki.meeting.identity.application;

public interface PasswordHasher {
  String hash(String password);

  boolean matches(String password, String hash);
}
