package com.dki.meeting.config;

import com.dki.meeting.booking.application.port.in.*;
import com.dki.meeting.booking.application.port.out.*;
import com.dki.meeting.booking.application.service.*;
import com.dki.meeting.booking.application.usecase.*;
import com.dki.meeting.booking.domain.*;
import com.dki.meeting.foundation.*;
import com.dki.meeting.identity.application.RefreshReuseException;
import com.dki.meeting.identity.application.port.in.*;
import com.dki.meeting.identity.application.port.out.*;
import com.dki.meeting.identity.application.service.*;
import com.dki.meeting.identity.application.usecase.*;
import com.dki.meeting.infra.module.*;
import com.dki.meeting.infra.transaction.TransactionalUseCase;
import com.dki.meeting.room.application.port.in.*;
import com.dki.meeting.room.application.port.out.*;
import com.dki.meeting.room.application.service.*;
import com.dki.meeting.room.application.usecase.*;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class UseCaseConfiguration {
  @Bean
  DailyRecurrenceStrategy dailyRecurrenceStrategy() {
    return new DailyRecurrenceStrategy();
  }

  @Bean
  WeeklyRecurrenceStrategy weeklyRecurrenceStrategy() {
    return new WeeklyRecurrenceStrategy();
  }

  @Bean
  MeetingPolicy meetingPolicy(List<RecurrenceStrategy> strategies) {
    return new MeetingPolicy(strategies);
  }

  @Bean
  IdentityService identityService(
      IdentityRepository repo,
      PasswordHasher hasher,
      AccessTokens tokens,
      Clock clock,
      AuditTrail audit) {
    return new IdentityService(repo, hasher, tokens, clock, audit);
  }

  @Bean
  RoomService roomService(
      RoomRepository repo, Clock clock, List<RoomUsage> usage, AuditTrail audit) {
    return new RoomService(repo, clock, usage, audit);
  }

  @Bean
  BookingRooms bookingRooms(RoomService rooms) {
    return new BookingRoomAdapter(rooms);
  }

  @Bean
  BookingEmployees bookingEmployees(IdentityService users) {
    return new BookingEmployeeAdapter(users);
  }

  @Bean
  BookingService bookingService(
      BookingRepository repo,
      BookingRooms rooms,
      BookingEmployees users,
      Clock clock,
      MeetingPolicy policy) {
    return new BookingService(repo, rooms, users, clock, policy);
  }

  @Bean
  IdentityHandlers identityHandlers(IdentityService service, PlatformTransactionManager manager) {
    return new IdentityHandlers(
        new TransactionalUseCase<>(new LoginUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new RefreshUseCase(service),
            manager,
            false,
            error -> error instanceof RefreshReuseException),
        new TransactionalUseCase<>(new LogoutUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new GetCurrentAccountUseCase(service), manager, true, error -> false),
        new TransactionalUseCase<>(new ListAccountsUseCase(service), manager, true, error -> false),
        new TransactionalUseCase<>(
            new SearchEmployeeDirectoryUseCase(service), manager, true, error -> false),
        new TransactionalUseCase<>(
            new CreateAccountUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new UpdateAccountUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new ChangePasswordUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new BootstrapAdministratorUseCase(service), manager, false, error -> false));
  }

  @Bean
  RoomHandlers roomHandlers(RoomService service, PlatformTransactionManager manager) {
    return new RoomHandlers(
        new TransactionalUseCase<>(new ListRoomsUseCase(service), manager, true, error -> false),
        new TransactionalUseCase<>(new GetRoomUseCase(service), manager, true, error -> false),
        new TransactionalUseCase<>(new CreateRoomUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(new UpdateRoomUseCase(service), manager, false, error -> false));
  }

  @Bean
  BookingHandlers bookingHandlers(BookingService service, PlatformTransactionManager manager) {
    return new BookingHandlers(
        new TransactionalUseCase<>(
            new CreateBookingUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new UpdateBookingUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new ApproveBookingUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new RejectBookingUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new CancelBookingUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(new GetBookingUseCase(service), manager, true, error -> false),
        new TransactionalUseCase<>(new ListBookingsUseCase(service), manager, true, error -> false),
        new TransactionalUseCase<>(
            new RespondToInvitationUseCase(service), manager, false, error -> false),
        new TransactionalUseCase<>(
            new GetRoomCalendarUseCase(service), manager, true, error -> false),
        new TransactionalUseCase<>(
            new FindAvailableRoomsUseCase(service), manager, true, error -> false));
  }
}
