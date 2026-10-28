package com.dki.meeting.booking.application;

import com.dki.meeting.booking.domain.*;
import com.dki.meeting.booking.domain.Booking.*;
import com.dki.meeting.foundation.*;
import com.dki.meeting.identity.application.IdentityService;
import com.dki.meeting.room.application.RoomService;
import com.dki.meeting.room.application.RoomView;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class BookingService {
  public record Input(
      UUID roomId,
      String title,
      String description,
      TimeWindow first,
      Recurrence recurrence,
      List<UUID> attendeeIds) {}

  public record Suggestion(
      String type, UUID roomId, String roomName, List<TimeWindow> occurrences) {}

  private final BookingRepository repo;
  private final RoomService rooms;
  private final IdentityService users;
  private final Clock clock;
  private final MeetingPolicy policy = new MeetingPolicy();

  public BookingService(
      BookingRepository repo, RoomService rooms, IdentityService users, Clock clock) {
    this.repo = repo;
    this.rooms = rooms;
    this.users = users;
    this.clock = clock;
  }

  @Transactional
  public Booking create(Actor actor, Input input) {
    RoomView room = rooms.lockActive(input.roomId());
    var windows = policy.expand(input.first(), input.recurrence(), clock.instant());
    validateAttendees(actor.id(), input.attendeeIds(), room);
    assertAvailable(room, windows, null, input.attendeeIds().size() + 1, actor);
    Instant now = clock.instant();
    var b =
        new Booking(
            UUID.randomUUID(),
            room.id(),
            actor.id(),
            input.title().trim(),
            input.description(),
            "pending",
            input.recurrence(),
            0,
            now,
            now,
            null,
            null,
            occurrences(windows),
            attendees(input.attendeeIds()));
    repo.insert(b, actor.id());
    repo.audit(b.id(), actor.id(), "booking_created", null);
    return find(b.id(), false);
  }

  @Transactional
  public Booking update(Actor actor, UUID id, Input input, long version) {
    Booking current = find(id, true);
    manage(actor, current);
    version(current, version);
    editable(current);
    RoomView room = rooms.lockActive(input.roomId());
    var windows = policy.expand(input.first(), input.recurrence(), clock.instant());
    validateAttendees(current.organizerId(), input.attendeeIds(), room);
    assertAvailable(room, windows, id, input.attendeeIds().size() + 1, actor);
    var next =
        new Booking(
            id,
            room.id(),
            current.organizerId(),
            input.title().trim(),
            input.description(),
            "pending",
            input.recurrence(),
            current.version() + 1,
            current.createdAt(),
            clock.instant(),
            null,
            null,
            occurrences(windows),
            attendees(input.attendeeIds()));
    repo.replace(next, actor.id());
    repo.audit(id, actor.id(), "booking_updated", "Approval is required again");
    return find(id, false);
  }

  @Transactional
  public Booking approve(Actor actor, UUID id, long expected) {
    actor.requireAdmin();
    Booking b = find(id, true);
    version(b, expected);
    requirePending(b);
    RoomView room = rooms.lockActive(b.roomId());
    users.active(b.organizerId());
    validateAttendees(b.organizerId(), b.attendees().stream().map(Attendee::userId).toList(), room);
    b.windows().forEach(w -> policy.validate(w, clock.instant(), true));
    assertAvailable(room, b.windows(), id, b.attendees().size() + 1, actor);
    repo.changeStatus(id, "approved", actor.id(), null, clock.instant());
    repo.audit(id, actor.id(), "booking_approved", null);
    return find(id, false);
  }

  @Transactional
  public Booking reject(Actor actor, UUID id, long expected, String reason) {
    actor.requireAdmin();
    if (reason == null || reason.isBlank())
      throw AppException.invalid("A rejection reason is required");
    Booking b = find(id, true);
    version(b, expected);
    requirePending(b);
    repo.changeStatus(id, "rejected", actor.id(), reason, clock.instant());
    repo.audit(id, actor.id(), "booking_rejected", reason);
    return find(id, false);
  }

  @Transactional
  public Booking cancel(Actor actor, UUID id, long expected, String reason) {
    Booking b = find(id, true);
    manage(actor, b);
    version(b, expected);
    if (!Set.of("pending", "approved").contains(b.status()))
      throw AppException.invalid("Only pending or approved bookings can be canceled");
    if (b.windows().stream().noneMatch(w -> w.startsAt().isAfter(clock.instant())))
      throw AppException.invalid("This booking has no future occurrence to cancel");
    repo.changeStatus(id, "cancelled", actor.id(), reason, clock.instant());
    repo.audit(id, actor.id(), "booking_cancelled", reason);
    return find(id, false);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Booking get(Actor actor, UUID id) {
    Booking b = find(id, false);
    view(actor, b);
    return b;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PageResult<Booking> list(Actor actor, String scope, String status, int page, int size) {
    PageResult.offset(page, size);
    if (!Set.of("mine", "invited", "all").contains(scope))
      throw new AppException(400, "Validation.Scope", "scope must be mine, invited or all");
    if (scope.equals("all")) actor.requireAdmin();
    if (!status.isBlank()
        && !Set.of("pending", "approved", "rejected", "cancelled").contains(status))
      throw new AppException(400, "Validation.Status", "Unknown booking status");
    return repo.list(actor, scope, status, page, size);
  }

  private Booking find(UUID id, boolean lock) {
    return repo.byId(id, lock).orElseThrow(() -> AppException.notFound("Booking"));
  }

  private void manage(Actor a, Booking b) {
    if (!a.isAdmin() && !b.organizerId().equals(a.id())) throw AppException.forbidden();
  }

  private void view(Actor a, Booking b) {
    if (!a.isAdmin()
        && !b.organizerId().equals(a.id())
        && b.attendees().stream().noneMatch(t -> t.userId().equals(a.id())))
      throw AppException.forbidden();
  }

  private void version(Booking b, long expected) {
    if (b.version() != expected)
      throw AppException.conflict(
          "Concurrency.StaleVersion", "The booking has changed. Reload and retry");
  }

  private void requirePending(Booking b) {
    if (!b.status().equals("pending"))
      throw AppException.invalid("Only pending bookings can be reviewed");
  }

  private void editable(Booking b) {
    if (!Set.of("pending", "approved").contains(b.status())
        || b.windows().stream().anyMatch(w -> !w.startsAt().isAfter(clock.instant())))
      throw AppException.invalid("Only unstarted pending or approved bookings can be edited");
  }

  private void validateAttendees(UUID organizer, List<UUID> ids, RoomView room) {
    if (new HashSet<>(ids).size() != ids.size() || ids.contains(organizer))
      throw AppException.invalid("Attendees must be unique and must exclude the organizer");
    if (ids.size() + 1 > room.capacity())
      throw AppException.invalid("The organizer and attendees exceed the room capacity");
    users.validateInvitees(ids);
  }

  private List<Occurrence> occurrences(List<TimeWindow> windows) {
    return windows.stream()
        .map(w -> new Occurrence(UUID.randomUUID(), w.startsAt(), w.endsAt(), "pending"))
        .toList();
  }

  private List<Attendee> attendees(List<UUID> ids) {
    return ids.stream().map(id -> new Attendee(id, "pending")).toList();
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
      RoomView room, List<TimeWindow> windows, UUID exclude, int capacity, Actor actor) {
    var conflicts = conflicts(room.id(), windows, exclude);
    if (conflicts.isEmpty()) return;
    var suggestions = new ArrayList<Suggestion>();
    rooms.list(actor, false).stream()
        .filter(
            r ->
                !r.id().equals(room.id())
                    && r.capacity() >= capacity
                    && r.equipment().containsAll(room.equipment()))
        .sorted(Comparator.comparingInt(RoomView::capacity).thenComparing(RoomView::name))
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
        409,
        "Booking.ScheduleConflict",
        "The room is unavailable for one or more occurrences",
        Map.of("unavailableOccurrences", conflicts, "suggestions", suggestions));
  }
}
