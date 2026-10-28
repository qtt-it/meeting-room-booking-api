package com.dki.meeting.booking.application.usecase;

import com.dki.meeting.booking.application.model.BookingCommands;
import com.dki.meeting.booking.application.model.BookingView;
import com.dki.meeting.booking.application.service.BookingMapper;
import com.dki.meeting.booking.application.service.BookingService;
import com.dki.meeting.foundation.OperationContext;
import com.dki.meeting.foundation.PageResult;
import com.dki.meeting.foundation.UseCase;

public final class ListBookingsUseCase
    implements UseCase<BookingCommands.Page, PageResult<BookingView>> {
  private final BookingService service;

  public ListBookingsUseCase(BookingService service) {
    this.service = service;
  }

  @Override
  public PageResult<BookingView> execute(OperationContext context, BookingCommands.Page input) {
    return page(
        service.list(
            context.requireActor(), input.scope(), input.status(), input.page(), input.pageSize()));
  }

  private PageResult<BookingView> page(PageResult<com.dki.meeting.booking.domain.Booking> source) {
    return new PageResult<>(
        source.items().stream().map(BookingMapper::toView).toList(),
        source.totalItems(),
        source.page(),
        source.pageSize());
  }
}
