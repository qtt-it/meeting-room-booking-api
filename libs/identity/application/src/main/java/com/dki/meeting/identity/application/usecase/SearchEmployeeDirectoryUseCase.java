package com.dki.meeting.identity.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.PageResult;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.identity.application.model.IdentityCommands;
import com.dki.meeting.identity.application.model.IdentityModels.UserView;
import com.dki.meeting.identity.application.service.IdentityService;

public final class SearchEmployeeDirectoryUseCase
    implements UseCase<IdentityCommands.Directory, PageResult<UserView>> {
  private final IdentityService service;

  public SearchEmployeeDirectoryUseCase(IdentityService service) {
    this.service = service;
  }

  @Override
  public PageResult<UserView> execute(OperationContext context, IdentityCommands.Directory input) {
    context.requireActor();
    return service.directory(input.keyword(), input.page(), input.pageSize());
  }
}
