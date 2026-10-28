package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.service.IdentityService;

public final class ChangePasswordUseCase implements UseCase<IdentityCommands.PasswordChange, Void> {
  private final IdentityService service;

  public ChangePasswordUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public Void execute(OperationContext context, IdentityCommands.PasswordChange input) {
    service.changePassword(context.requireActor(), input.currentPassword(), input.newPassword());
    return null;
  }
}
