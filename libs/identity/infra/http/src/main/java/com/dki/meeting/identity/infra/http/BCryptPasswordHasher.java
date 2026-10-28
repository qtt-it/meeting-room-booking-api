package com.dki.meeting.identity.infra.http;

import com.dki.meeting.foundation.AppException;
import com.dki.meeting.identity.application.port.out.PasswordHasher;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

  public String hash(String value) {
    if (value == null || value.length() < 12 || value.getBytes(StandardCharsets.UTF_8).length > 72)
      throw AppException.invalid(
          "Passwords must contain at least 12 characters and at most 72 UTF-8 bytes");
    return encoder.encode(value);
  }

  public boolean matches(String value, String hash) {
    return value != null
        && value.getBytes(StandardCharsets.UTF_8).length <= 72
        && encoder.matches(value, hash);
  }
}
