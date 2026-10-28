package com.dki.meeting.room.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.PageResult;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.room.application.RoomView;
import com.dki.meeting.room.application.model.RoomCommands;
import com.dki.meeting.room.application.service.RoomService;

public final class ListRoomsUseCase implements UseCase<RoomCommands.Catalog, PageResult<RoomView>> {
  private final RoomService service;

  public ListRoomsUseCase(RoomService service) {
    this.service = service;
  }

  @Override
  public PageResult<RoomView> execute(OperationContext context, RoomCommands.Catalog input) {
    return service.catalog(
        context.requireActor(), input.includeInactive(), input.page(), input.pageSize());
  }
}
