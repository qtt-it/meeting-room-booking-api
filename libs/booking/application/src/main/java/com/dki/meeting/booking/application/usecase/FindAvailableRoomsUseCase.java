package com.dki.meeting.booking.application.usecase;

import com.dki.meeting.booking.application.model.BookingCommands;
import com.dki.meeting.booking.application.model.RoomSummary;
import com.dki.meeting.booking.application.service.BookingService;
import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import java.util.List;

public final class FindAvailableRoomsUseCase
    implements UseCase<BookingCommands.Availability, List<RoomSummary>> {
  private final BookingService service;

  public FindAvailableRoomsUseCase(BookingService service) {
    this.service = service;
  }

  @Override
  public List<RoomSummary> execute(OperationContext context, BookingCommands.Availability input) {
    return service.available(
        context.requireActor(),
        input.window(),
        input.minCapacity(),
        input.location(),
        input.floor(),
        input.equipment());
  }
}
