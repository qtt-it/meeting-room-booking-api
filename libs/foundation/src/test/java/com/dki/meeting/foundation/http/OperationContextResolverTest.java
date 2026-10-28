package com.dki.meeting.foundation.http;

import static org.assertj.core.api.Assertions.*;

import com.dki.meeting.foundation.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class OperationContextResolverTest {
  private final OperationContextResolver resolver = new OperationContextResolver();

  private static class Fixture {
    void annotated(@CurrentOperation OperationContext context) {}

    void unannotated(OperationContext context) {}
  }

  @AfterEach
  void clear() {
    MDC.clear();
    SecurityContextHolder.clearContext();
  }

  private MethodParameter parameter(String method) throws Exception {
    return new MethodParameter(Fixture.class.getDeclaredMethod(method, OperationContext.class), 0);
  }

  @Test
  void resolvesVerifiedActorAndRequestIdAtTheHttpBoundary() throws Exception {
    var actor = new Actor(UUID.randomUUID(), "user");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
    MDC.put("requestId", "request-123");
    assertThat(resolver.supportsParameter(parameter("annotated"))).isTrue();
    var context =
        (OperationContext) resolver.resolveArgument(parameter("annotated"), null, null, null);
    assertThat(context.actor()).isEqualTo(actor);
    assertThat(context.requestId()).isEqualTo("request-123");
  }

  @Test
  void anonymousContextCannotBecomeAnAuthenticatedActor() throws Exception {
    var context =
        (OperationContext) resolver.resolveArgument(parameter("annotated"), null, null, null);
    assertThat(context.actor()).isNull();
    assertThatThrownBy(context::requireActor).isInstanceOf(AppException.class);
  }

  @Test
  void onlyAnnotatedContextParametersUseThisResolver() throws Exception {
    assertThat(resolver.supportsParameter(parameter("unannotated"))).isFalse();
  }
}
