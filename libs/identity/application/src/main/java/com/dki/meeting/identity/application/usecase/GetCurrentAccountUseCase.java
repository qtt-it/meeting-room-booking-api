package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityModels.UserView;
import com.dki.meeting.identity.application.service.IdentityService;

public final class GetCurrentAccountUseCase implements UseCase<Void, UserView> {
  private final IdentityService service;

  public GetCurrentAccountUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public UserView execute(OperationContext context, Void input) {
    return service.me(context.requireActor());
  }
}
