package com.dki.meeting.booking.application.usecase;

import com.dki.meeting.booking.application.model.BookingView;
import com.dki.meeting.booking.application.service.BookingMapper;
import com.dki.meeting.booking.application.service.BookingService;
import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.UseCase;
import java.util.UUID;

public final class GetBookingUseCase implements UseCase<UUID, BookingView> {
  private final BookingService service;

  public GetBookingUseCase(BookingService service) {
    this.service = service;
  }

  @Override
  public BookingView execute(OperationContext context, UUID input) {
    return BookingMapper.toView(service.get(context.requireActor(), input));
  }
}
