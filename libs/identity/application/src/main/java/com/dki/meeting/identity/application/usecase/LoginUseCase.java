package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.model.IdentityModels.Tokens;
import com.dki.meeting.identity.application.service.IdentityService;

public final class LoginUseCase implements UseCase<IdentityCommands.Login, Tokens> {
  private final IdentityService service;

  public LoginUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public Tokens execute(OperationContext context, IdentityCommands.Login input) {
    return service.login(input.email(), input.password());
  }
}
