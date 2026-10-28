package com.dki.meeting.identity.infra.http;

import static org.assertj.core.api.Assertions.*;

import com.dki.meeting.identity.application.IdentityService;
import org.junit.jupiter.api.Test;

class CredentialRedactionTest {
  @Test
  void requestAndResponseDiagnosticsDoNotExposeCredentials() {
    String secret = "DoNotLogThisCredential";
    assertThat(new IdentityController.Login("person@example.com", secret).toString())
        .doesNotContain(secret, "person@example.com");
    assertThat(new IdentityController.Refresh(secret).toString()).doesNotContain(secret);
    assertThat(
            new IdentityController.CreateUser("person@example.com", "Person", secret, "user")
                .toString())
        .doesNotContain(secret);
    assertThat(new IdentityController.PasswordChange(secret, secret).toString())
        .doesNotContain(secret);
    assertThat(new IdentityService.Tokens(secret, secret, "Bearer", 900, null).toString())
        .doesNotContain(secret);
  }
}
