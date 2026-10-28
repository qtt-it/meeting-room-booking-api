package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.model.IdentityModels.Tokens;
import com.dki.meeting.identity.application.service.IdentityService;

public final class RefreshUseCase implements UseCase<IdentityCommands.Refresh, Tokens> {
  private final IdentityService service;

  public RefreshUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public Tokens execute(OperationContext context, IdentityCommands.Refresh input) {
    return service.refresh(input.refreshToken());
  }
}
