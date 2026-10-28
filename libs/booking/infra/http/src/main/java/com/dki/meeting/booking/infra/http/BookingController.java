package com.dki.meeting.booking.infra.http;

import com.dki.meeting.booking.application.model.*;
import com.dki.meeting.booking.application.port.in.BookingHandlers;
import com.dki.meeting.booking.domain.*;
import com.dki.meeting.foundation.*;
import com.dki.meeting.foundation.http.AdminOnly;
import com.dki.meeting.foundation.http.ApiResponse;
import com.dki.meeting.foundation.http.CurrentOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
  private final BookingHandlers handlers;

  public BookingController(BookingHandlers handlers) {
    this.handlers = handlers;
  }

  public record RecurrenceInput(
      @NotNull @Pattern(regexp = "daily|weekly") String frequency,
      @Min(1) @Max(12) int interval,
      @Min(2) @Max(52) int count) {
    Recurrence value() {
      return new Recurrence(frequency, interval, count);
    }
  }

  public record BookingInput(
      @NotNull UUID roomId,
      @NotBlank @Size(max = 150) String title,
      @Size(max = 2000) String description,
      @NotNull Instant startsAt,
      @NotNull Instant endsAt,
      @Valid RecurrenceInput recurrence,
      @NotNull @Size(max = 199) List<@NotNull UUID> attendeeIds,
      @PositiveOrZero Long version) {
    com.dki.meeting.booking.application.model.BookingModels.Input input() {
      return new com.dki.meeting.booking.application.model.BookingModels.Input(
          roomId,
          title,
          description,
          new TimeWindow(startsAt, endsAt),
          recurrence == null ? null : recurrence.value(),
          attendeeIds);
    }
  }

  public record Decision(@NotNull @PositiveOrZero Long version, @Size(max = 1000) String reason) {}

  public record InvitationResponse(
      @NotNull @Pattern(regexp = "accepted|declined") String response) {}

  @PostMapping("/bookings")
  public ResponseEntity<?> create(
      @CurrentOperation OperationContext context, @Valid @RequestBody BookingInput r) {
    var b = handlers.create().execute(context, r.input());
    return ResponseEntity.created(java.net.URI.create("/api/v1/bookings/" + b.id()))
        .body(ApiResponse.ok(b, "Booking submitted for approval"));
  }

  @PutMapping("/bookings/{id}")
  public ApiResponse<?> update(
      @CurrentOperation OperationContext context,
      @PathVariable UUID id,
      @Valid @RequestBody BookingInput r) {
    if (r.version() == null)
      throw new AppException(
          FailureKind.INVALID_REQUEST, "Validation.InvalidRequest", "version is required");
    return ApiResponse.ok(
        handlers.update().execute(context, new BookingCommands.Update(id, r.input(), r.version())),
        "Booking updated and submitted for approval");
  }

  @GetMapping("/bookings/{id}")
  public ApiResponse<?> get(@CurrentOperation OperationContext context, @PathVariable UUID id) {
    return ApiResponse.ok(handlers.get().execute(context, id), "Retrieved successfully");
  }

  @GetMapping("/bookings")
  public ApiResponse<?> list(
      @CurrentOperation OperationContext context,
      @RequestParam(defaultValue = "mine") String scope,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(
        handlers.list().execute(context, new BookingCommands.Page(scope, status, page, pageSize)));
  }

  @AdminOnly
  @PostMapping("/bookings/{id}/approval")
  public ApiResponse<?> approve(
      @CurrentOperation OperationContext context,
      @PathVariable UUID id,
      @Valid @RequestBody Decision r) {
    return ApiResponse.ok(
        handlers.approve().execute(context, new BookingCommands.Decision(id, r.version(), null)),
        "Booking approved");
  }

  @AdminOnly
  @PostMapping("/bookings/{id}/rejection")
  public ApiResponse<?> reject(
      @CurrentOperation OperationContext context,
      @PathVariable UUID id,
      @Valid @RequestBody Decision r) {
    return ApiResponse.ok(
        handlers
            .reject()
            .execute(context, new BookingCommands.Decision(id, r.version(), r.reason())),
        "Booking rejected");
  }

  @PostMapping("/bookings/{id}/cancellation")
  public ApiResponse<?> cancel(
      @CurrentOperation OperationContext context,
      @PathVariable UUID id,
      @Valid @RequestBody Decision r) {
    return ApiResponse.ok(
        handlers
            .cancel()
            .execute(context, new BookingCommands.Decision(id, r.version(), r.reason())),
        "Future occurrences canceled");
  }

  @PutMapping("/bookings/{id}/invitation-response")
  public ApiResponse<?> respond(
      @CurrentOperation OperationContext context,
      @PathVariable UUID id,
      @Valid @RequestBody InvitationResponse r) {
    return ApiResponse.ok(
        handlers.respond().execute(context, new BookingCommands.Respond(id, r.response())),
        "Invitation response recorded");
  }

  @GetMapping("/rooms/{id}/calendar")
  public ApiResponse<?> calendar(
      @CurrentOperation OperationContext context,
      @PathVariable UUID id,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(
        handlers
            .calendar()
            .execute(context, new BookingCommands.Calendar(id, from, to, page, pageSize)));
  }

  @GetMapping("/rooms/availability")
  public ApiResponse<?> available(
      @CurrentOperation OperationContext context,
      @RequestParam Instant startsAt,
      @RequestParam Instant endsAt,
      @RequestParam(defaultValue = "1") int minCapacity,
      @RequestParam(defaultValue = "") @Size(max = 100) String location,
      @RequestParam(defaultValue = "") @Size(max = 30) String floor,
      @RequestParam(defaultValue = "") @Size(max = 30)
          List<@NotBlank @Size(max = 50) String> equipment) {
    return ApiResponse.ok(
        handlers
            .available()
            .execute(
                context,
                new BookingCommands.Availability(
                    new TimeWindow(startsAt, endsAt), minCapacity, location, floor, equipment)),
        "Retrieved successfully");
  }
}
