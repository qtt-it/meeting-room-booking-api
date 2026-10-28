package com.dki.meeting.booking.application.service;

import com.dki.meeting.booking.application.model.BookingModels.*;
import com.dki.meeting.booking.application.model.BusySlot;
import com.dki.meeting.booking.application.model.RoomSummary;
import com.dki.meeting.booking.application.port.out.*;
import com.dki.meeting.booking.domain.*;
import com.dki.meeting.booking.domain.Booking.*;
import com.dki.meeting.foundation.*;
import java.time.*;
import java.util.*;

public class BookingService {
  private final BookingRepository repo;
  private final BookingRooms rooms;
  private final BookingEmployees users;
  private final Clock clock;
  private final MeetingPolicy policy;
  private final BookingAuthorizer authorizer = new BookingAuthorizer();

  public BookingService(
      BookingRepository repo,
      BookingRooms rooms,
      BookingEmployees users,
      Clock clock,
      MeetingPolicy policy) {
    this.repo = repo;
    this.rooms = rooms;
    this.users = users;
    this.clock = clock;
    this.policy = policy;
  }

  public Booking create(Actor actor, Input input) {
    RoomSummary room = rooms.lockActive(input.roomId());
    var windows = policy.expand(input.first(), input.recurrence(), clock.instant());
    validateAttendees(actor.id(), input.attendeeIds(), room);
    assertAvailable(room, windows, null, input.attendeeIds().size() + 1);
    Instant now = clock.instant();
    var b =
        BookingFactory.create(
            UUID.randomUUID(),
            actor.id(),
            new BookingDraft(
                room.id(),
                input.title(),
                input.description(),
                input.recurrence(),
                windows,
                input.attendeeIds()),
            now,
            UUID::randomUUID);
    repo.insert(b, actor.id());
    repo.audit(b.id(), actor.id(), "booking_created", null);
    return find(b.id(), false);
  }

  public Booking update(Actor actor, UUID id, Input input, long version) {
    Booking current = find(id, true);
    authorizer.requireManager(actor, current);
    current.requireVersion(version);
    current.requireEditable(clock.instant());
    RoomSummary room = rooms.lockActive(input.roomId());
    var windows = policy.expand(input.first(), input.recurrence(), clock.instant());
    validateAttendees(current.organizerId(), input.attendeeIds(), room);
    assertAvailable(room, windows, id, input.attendeeIds().size() + 1);
    var next =
        BookingFactory.reschedule(
            current,
            new BookingDraft(
                room.id(),
                input.title(),
                input.description(),
                input.recurrence(),
                windows,
                input.attendeeIds()),
            clock.instant(),
            UUID::randomUUID);
    repo.replace(next, actor.id());
    repo.audit(id, actor.id(), "booking_updated", "Approval is required again");
    return find(id, false);
  }

  public Booking approve(Actor actor, UUID id, long expected) {
    actor.requireAdmin();
    Booking b = find(id, true);
    b.requireVersion(expected);
    b.requirePending();
    RoomSummary room = rooms.lockActive(b.roomId());
    users.requireActive(b.organizerId());
    validateAttendees(b.organizerId(), b.attendees().stream().map(Attendee::userId).toList(), room);
    b.windows().forEach(w -> policy.validate(w, clock.instant(), true));
    assertAvailable(room, b.windows(), id, b.attendees().size() + 1);
    repo.changeStatus(id, "approved", actor.id(), null, clock.instant());
    repo.audit(id, actor.id(), "booking_approved", null);
    return find(id, false);
  }

  public Booking reject(Actor actor, UUID id, long expected, String reason) {
    actor.requireAdmin();
    if (reason == null || reason.isBlank())
      throw AppException.invalid("A rejection reason is required");
    Booking b = find(id, true);
    b.requireVersion(expected);
    b.requirePending();
    repo.changeStatus(id, "rejected", actor.id(), reason, clock.instant());
    repo.audit(id, actor.id(), "booking_rejected", reason);
    return find(id, false);
  }

  public Booking cancel(Actor actor, UUID id, long expected, String reason) {
    Booking b = find(id, true);
    authorizer.requireManager(actor, b);
    b.requireVersion(expected);
    b.requireCancelable(clock.instant());
    repo.changeStatus(id, "cancelled", actor.id(), reason, clock.instant());
    repo.audit(id, actor.id(), "booking_cancelled", reason);
    return find(id, false);
  }

  public Booking get(Actor actor, UUID id) {
    Booking b = find(id, false);
    authorizer.requireViewer(actor, b);
    return b;
  }

  public PageResult<Booking> list(Actor actor, String scope, String status, int page, int size) {
    PageResult.offset(page, size);
    if (!Set.of("mine", "invited", "all").contains(scope))
      throw new AppException(
          FailureKind.INVALID_REQUEST, "Validation.Scope", "scope must be mine, invited or all");
    if (scope.equals("all")) actor.requireAdmin();
    if (!status.isBlank()
        && !Set.of("pending", "approved", "rejected", "cancelled").contains(status))
      throw new AppException(
          FailureKind.INVALID_REQUEST, "Validation.Status", "Unknown booking status");
    return repo.list(actor, scope, status, page, size);
  }

  public Booking respond(Actor actor, UUID id, String response) {
    if (!Set.of("accepted", "declined").contains(response))
      throw AppException.invalid("The invitation response must be accepted or declined");
    Booking b = find(id, true);
    if (b.attendees().stream().noneMatch(a -> a.userId().equals(actor.id())))
      throw AppException.forbidden();
    b.requireAcceptingResponses(clock.instant());
    if (!repo.respond(id, actor.id(), response)) throw AppException.forbidden();
    repo.audit(id, actor.id(), "invitation_responded", response);
    return find(id, false);
  }

  public PageResult<BusySlot> calendar(UUID room, Instant from, Instant to, int page, int size) {
    PageResult.offset(page, size);
    rooms.find(room);
    TimeWindow range = new TimeWindow(from, to);
    if (Duration.between(range.startsAt(), range.endsAt()).compareTo(Duration.ofDays(31)) > 0)
      throw AppException.invalid("Calendar ranges cannot exceed 31 days");
    return repo.calendar(room, from, to, page, size);
  }

  public List<RoomSummary> available(
      Actor actor,
      TimeWindow w,
      int capacity,
      String location,
      String floor,
      List<String> equipment) {
    policy.validate(w, clock.instant(), true);
    return rooms.activeRooms().stream()
        .filter(new RoomRequirements(capacity, location, floor, equipment))
        .filter(r -> conflicts(r.id(), List.of(w), null).isEmpty())
        .toList();
  }

  private Booking find(UUID id, boolean lock) {
    return repo.byId(id, lock).orElseThrow(() -> AppException.notFound("Booking"));
  }

  private void validateAttendees(UUID organizer, List<UUID> ids, RoomSummary room) {
    if (new HashSet<>(ids).size() != ids.size() || ids.contains(organizer))
      throw AppException.invalid("Attendees must be unique and must exclude the organizer");
    if (ids.size() + 1 > room.capacity())
      throw AppException.invalid("The organizer and attendees exceed the room capacity");
    users.validateInvitees(ids);
  }

  private List<TimeWindow> conflicts(UUID room, List<TimeWindow> windows, UUID exclude) {
    Instant start =
        windows.stream().map(TimeWindow::startsAt).min(Comparator.naturalOrder()).orElseThrow();
    Instant end =
        windows.stream().map(TimeWindow::endsAt).max(Comparator.naturalOrder()).orElseThrow();
    List<TimeWindow> occupied = repo.occupied(room, start, end, exclude);
    return windows.stream().filter(w -> occupied.stream().anyMatch(w::overlaps)).toList();
  }

  private void assertAvailable(
      RoomSummary room, List<TimeWindow> windows, UUID exclude, int capacity) {
    var conflicts = conflicts(room.id(), windows, exclude);
    if (conflicts.isEmpty()) return;
    var suggestions = new ArrayList<Suggestion>();
    rooms.activeRooms().stream()
        .filter(r -> !r.id().equals(room.id()))
        .filter(new RoomRequirements(capacity, "", "", room.equipment()))
        .sorted(Comparator.comparingInt(RoomSummary::capacity).thenComparing(RoomSummary::name))
        .filter(r -> conflicts(r.id(), windows, exclude).isEmpty())
        .limit(3)
        .forEach(r -> suggestions.add(new Suggestion("other_room", r.id(), r.name(), windows)));
    for (int shift = 15; shift <= 180 && suggestions.size() < 5; shift += 15) {
      final Duration delta = Duration.ofMinutes(shift);
      var shifted = windows.stream().map(w -> w.shift(delta)).toList();
      try {
        shifted.forEach(w -> policy.validate(w, clock.instant(), true));
      } catch (AppException ignored) {
        continue;
      }
      if (conflicts(room.id(), shifted, exclude).isEmpty())
        suggestions.add(new Suggestion("later_time", room.id(), room.name(), shifted));
    }
    throw new AppException(
        FailureKind.CONFLICT,
        "Booking.ScheduleConflict",
        "The room is unavailable for one or more occurrences",
        Map.of("unavailableOccurrences", conflicts, "suggestions", suggestions));
  }
}
