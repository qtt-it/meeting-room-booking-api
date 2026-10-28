package com.dki.meeting.booking.application.usecase;

import com.dki.meeting.booking.application.model.BookingCommands;
import com.dki.meeting.booking.application.model.BookingView;
import com.dki.meeting.booking.application.service.BookingMapper;
import com.dki.meeting.booking.application.service.BookingService;
import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;

public final class UpdateBookingUseCase implements UseCase<BookingCommands.Update, BookingView> {
  private final BookingService service;

  public UpdateBookingUseCase(BookingService service) {
    this.service = service;
  }

  @Override
  public BookingView execute(OperationContext context, BookingCommands.Update input) {
    return BookingMapper.toView(
        service.update(context.requireActor(), input.id(), input.input(), input.version()));
  }
}
