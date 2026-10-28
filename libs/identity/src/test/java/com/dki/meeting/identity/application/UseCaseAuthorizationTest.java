package com.dki.meeting.identity.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dki.meeting.foundation.*;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.service.IdentityService;
import com.dki.meeting.identity.application.usecase.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UseCaseAuthorizationTest {
  private final IdentityService service = mock(IdentityService.class);

  @Test
  void directoryRequiresAnActorWithoutHttpSecurity() {
    var useCase = new SearchEmployeeDirectoryUseCase(service);
    assertThatThrownBy(
            () ->
                useCase.execute(
                    new OperationContext(null, "request"),
                    new IdentityCommands.Directory("", 1, 20)))
        .isInstanceOfSatisfying(
            AppException.class,
            failure -> assertThat(failure.kind()).isEqualTo(FailureKind.UNAUTHENTICATED));
    verifyNoInteractions(service);
  }

  @Test
  void administratorCannotInvokeSystemBootstrap() {
    var useCase = new BootstrapAdministratorUseCase(service);
    assertThatThrownBy(
            () ->
                useCase.execute(
                    new OperationContext(new Actor(UUID.randomUUID(), "admin"), "request"),
                    new IdentityCommands.Bootstrap("admin@example.com", "NotForLogging123!")))
        .isInstanceOfSatisfying(
            AppException.class,
            failure -> assertThat(failure.kind()).isEqualTo(FailureKind.FORBIDDEN));
    verifyNoInteractions(service);
  }
}
