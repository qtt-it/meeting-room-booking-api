package com.dki.meeting.room.application.usecase;

import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import com.dki.meeting.room.application.RoomView;
import com.dki.meeting.room.application.service.RoomService;
import java.util.UUID;

public final class GetRoomUseCase implements UseCase<UUID, RoomView> {
  private final RoomService service;

  public GetRoomUseCase(RoomService service) {
    this.service = service;
  }

  @Override
  public RoomView execute(OperationContext context, UUID input) {
    return service.getForActor(context.requireActor(), input);
  }
}
