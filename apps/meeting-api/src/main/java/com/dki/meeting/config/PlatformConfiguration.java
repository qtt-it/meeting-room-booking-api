package com.dki.meeting.config;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.port.in.IdentityHandlers;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
public class PlatformConfiguration {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  @ConditionalOnProperty(
      name = "app.bootstrap.enabled",
      havingValue = "true",
      matchIfMissing = true)
  ApplicationRunner bootstrap(
      IdentityHandlers handlers,
      @Value("${app.bootstrap.email:}") String email,
      @Value("${app.bootstrap.password:}") String password) {
    return args ->
        handlers
            .bootstrap()
            .execute(OperationContext.system(), new IdentityCommands.Bootstrap(email, password));
  }
}
