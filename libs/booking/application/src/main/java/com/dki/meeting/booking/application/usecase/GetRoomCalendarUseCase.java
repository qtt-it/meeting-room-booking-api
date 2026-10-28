package com.dki.meeting.booking.application.usecase;

import com.dki.meeting.booking.application.model.BookingCommands;
import com.dki.meeting.booking.application.model.BusySlot;
import com.dki.meeting.booking.application.service.BookingService;
import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.PageResult;
import com.dki.meeting.foundation.UseCase;

public final class GetRoomCalendarUseCase
    implements UseCase<BookingCommands.Calendar, PageResult<BusySlot>> {
  private final BookingService service;

  public GetRoomCalendarUseCase(BookingService service) {
    this.service = service;
  }

  @Override
  public PageResult<BusySlot> execute(OperationContext context, BookingCommands.Calendar input) {
    context.requireActor();
    return service.calendar(
        input.roomId(), input.from(), input.to(), input.page(), input.pageSize());
  }
}
