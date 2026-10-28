package com.dki.meeting.room.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.room.application.RoomView;
import com.dki.meeting.room.application.model.RoomCommands;
import com.dki.meeting.room.application.service.RoomService;

public final class UpdateRoomUseCase implements UseCase<RoomCommands.Update, RoomView> {
  private final RoomService service;

  public UpdateRoomUseCase(RoomService service) {
    this.service = service;
  }

  @Override
  public RoomView execute(OperationContext context, RoomCommands.Update input) {
    return service.update(
        context.requireActor(), input.id(), input.input(), input.active(), input.version());
  }
}
