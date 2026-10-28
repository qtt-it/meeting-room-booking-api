package com.dki.meeting.room.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.room.application.RoomView;
import com.dki.meeting.room.application.model.RoomCommands;
import com.dki.meeting.room.application.service.RoomService;

public final class CreateRoomUseCase implements UseCase<RoomCommands.Input, RoomView> {
  private final RoomService service;

  public CreateRoomUseCase(RoomService service) {
    this.service = service;
  }

  @Override
  public RoomView execute(OperationContext context, RoomCommands.Input input) {
    return service.create(context.requireActor(), input);
  }
}
