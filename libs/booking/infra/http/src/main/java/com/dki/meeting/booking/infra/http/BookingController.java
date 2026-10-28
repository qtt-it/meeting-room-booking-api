package com.dki.meeting.booking.infra.http;

import com.dki.meeting.booking.application.BookingService;
import com.dki.meeting.booking.domain.*;
import com.dki.meeting.foundation.*;
import com.dki.meeting.foundation.http.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
  private final BookingService service;

  public BookingController(BookingService service) {
    this.service = service;
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
    BookingService.Input input() {
      return new BookingService.Input(
          roomId,
          title,
          description,
          new TimeWindow(startsAt, endsAt),
          recurrence == null ? null : recurrence.value(),
          attendeeIds);
    }
  }

  @PostMapping("/bookings")
  public ResponseEntity<?> create(@Valid @RequestBody BookingInput r) {
    var b = service.create(SecurityActor.current(), r.input());
    return ResponseEntity.created(java.net.URI.create("/api/v1/bookings/" + b.id()))
        .body(ApiResponse.ok(b, "Booking submitted for approval"));
  }

  @PutMapping("/bookings/{id}")
  public ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody BookingInput r) {
    if (r.version() == null)
      throw new AppException(400, "Validation.InvalidRequest", "version is required");
    return ApiResponse.ok(
        service.update(SecurityActor.current(), id, r.input(), r.version()),
        "Booking updated and submitted for approval");
  }

  @GetMapping("/bookings/{id}")
  public ApiResponse<?> get(@PathVariable UUID id) {
    return ApiResponse.ok(service.get(SecurityActor.current(), id), "Retrieved successfully");
  }

  @GetMapping("/bookings")
  public ApiResponse<?> list(
      @RequestParam(defaultValue = "mine") String scope,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(service.list(SecurityActor.current(), scope, status, page, pageSize));
  }
}
