package com.dki.meeting.booking.application.usecase;

import com.dki.meeting.booking.application.model.BookingModels;
import com.dki.meeting.booking.application.model.BookingView;
import com.dki.meeting.booking.application.service.BookingMapper;
import com.dki.meeting.booking.application.service.BookingService;
import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;

public final class CreateBookingUseCase implements UseCase<BookingModels.Input, BookingView> {
  private final BookingService service;

  public CreateBookingUseCase(BookingService service) {
    this.service = service;
  }

  @Override
  public BookingView execute(OperationContext context, BookingModels.Input input) {
    return BookingMapper.toView(service.create(context.requireActor(), input));
  }
}
