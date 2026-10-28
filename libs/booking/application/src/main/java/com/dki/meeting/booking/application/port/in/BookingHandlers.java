package com.dki.meeting.booking.application.port.in;

import com.dki.meeting.booking.application.model.*;
import com.dki.meeting.booking.application.port.out.*;
import com.dki.meeting.foundation.*;
import java.util.*;

public record BookingHandlers(
    UseCase<BookingModels.Input, BookingView> create,
    UseCase<BookingCommands.Update, BookingView> update,
    UseCase<BookingCommands.Decision, BookingView> approve,
    UseCase<BookingCommands.Decision, BookingView> reject,
    UseCase<BookingCommands.Decision, BookingView> cancel,
    UseCase<UUID, BookingView> get,
    UseCase<BookingCommands.Page, PageResult<BookingView>> list,
    UseCase<BookingCommands.Respond, BookingView> respond,
    UseCase<BookingCommands.Calendar, PageResult<BookingRepository.BusySlot>> calendar,
    UseCase<BookingCommands.Availability, List<RoomSummary>> available) {}
