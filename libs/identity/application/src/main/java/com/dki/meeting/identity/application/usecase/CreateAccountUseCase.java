package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.model.IdentityModels.UserView;
import com.dki.meeting.identity.application.service.IdentityService;

public final class CreateAccountUseCase implements UseCase<IdentityCommands.Create, UserView> {
  private final IdentityService service;

  public CreateAccountUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public UserView execute(OperationContext context, IdentityCommands.Create input) {
    return service.create(
        context.requireActor(), input.email(), input.displayName(), input.password(), input.role());
  }
}
