package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.model.IdentityModels.UserView;
import com.dki.meeting.identity.application.service.IdentityService;

public final class UpdateAccountUseCase implements UseCase<IdentityCommands.Update, UserView> {
  private final IdentityService service;

  public UpdateAccountUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public UserView execute(OperationContext context, IdentityCommands.Update input) {
    return service.update(
        context.requireActor(),
        input.id(),
        input.displayName(),
        input.role(),
        input.active(),
        input.version());
  }
}
