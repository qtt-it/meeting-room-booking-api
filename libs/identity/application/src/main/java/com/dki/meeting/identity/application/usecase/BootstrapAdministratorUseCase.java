package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.AppException;
import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.service.IdentityService;

public final class BootstrapAdministratorUseCase
    implements UseCase<IdentityCommands.Bootstrap, Void> {
  private final IdentityService service;

  public BootstrapAdministratorUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public Void execute(OperationContext context, IdentityCommands.Bootstrap input) {
    if (!context.requireActor().role().equals("system")) throw AppException.forbidden();
    service.bootstrap(input.email(), input.password());
    return null;
  }
}
