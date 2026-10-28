package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.service.IdentityService;

public final class LogoutUseCase implements UseCase<IdentityCommands.Refresh, Void> {
  private final IdentityService service;

  public LogoutUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public Void execute(OperationContext context, IdentityCommands.Refresh input) {
    service.logout(input.refreshToken());
    return null;
  }
}
