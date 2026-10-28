package com.dki.meeting.booking.application.usecase;

import com.dki.meeting.booking.application.model.BookingCommands;
import com.dki.meeting.booking.application.model.BookingView;
import com.dki.meeting.booking.application.service.BookingMapper;
import com.dki.meeting.booking.application.service.BookingService;
import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;

public final class RejectBookingUseCase implements UseCase<BookingCommands.Decision, BookingView> {
  private final BookingService service;

  public RejectBookingUseCase(BookingService service) {
    this.service = service;
  }

  @Override
  public BookingView execute(OperationContext context, BookingCommands.Decision input) {
    return BookingMapper.toView(
        service.reject(context.requireActor(), input.id(), input.version(), input.reason()));
  }
}
