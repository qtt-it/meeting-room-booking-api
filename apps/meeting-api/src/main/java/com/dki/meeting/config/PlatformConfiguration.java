package com.dki.meeting.config;

import com.dki.meeting.identity.application.IdentityService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

@Configuration
public class PlatformConfiguration {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  ApplicationRunner bootstrap(
      IdentityService users,
      @Value("${app.bootstrap.email:}") String email,
      @Value("${app.bootstrap.password:}") String password) {
    return args -> users.bootstrap(email, password);
  }
}
