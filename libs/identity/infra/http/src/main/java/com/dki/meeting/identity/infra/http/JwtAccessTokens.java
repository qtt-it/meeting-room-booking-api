package com.dki.meeting.identity.infra.http;

import com.dki.meeting.identity.application.port.out.AccessTokens;
import com.dki.meeting.identity.domain.UserAccount;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokens implements AccessTokens {
  private final JwtEncoder encoder;
  private final Clock clock;

  public JwtAccessTokens(JwtEncoder encoder, Clock clock) {
    this.encoder = encoder;
    this.clock = clock;
  }

  public String issue(UserAccount u) {
    var claims =
        JwtClaimsSet.builder()
            .issuer("meeting-room-booking")
            .audience(List.of("meeting-api"))
            .subject(u.id().toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(clock.instant())
            .expiresAt(clock.instant().plusSeconds(expiresInSeconds()))
            .claim("sv", u.securityVersion())
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  public long expiresInSeconds() {
    return 900;
  }
}
