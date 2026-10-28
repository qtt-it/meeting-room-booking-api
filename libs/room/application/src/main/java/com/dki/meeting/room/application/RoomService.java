package com.dki.meeting.room.application;

import com.dki.meeting.foundation.*;
import com.dki.meeting.room.domain.Room;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
  public record Input(
      String name,
      String description,
      String location,
      String floor,
      int capacity,
      List<String> equipment) {}

  private final RoomRepository repo;
  private final Clock clock;
  private final AuditTrail audit;
  private final List<RoomUsage> usage;

  public RoomService(RoomRepository repo, Clock clock, List<RoomUsage> usage, AuditTrail audit) {
    this.repo = repo;
    this.clock = clock;
    this.audit = audit;
    this.usage = List.copyOf(usage);
  }

  public List<RoomView> list(Actor actor, boolean includeInactive) {
    if (includeInactive) actor.requireAdmin();
    return repo.list(includeInactive).stream().map(RoomView::from).toList();
  }

  @Transactional(
      readOnly = true,
      isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public PageResult<RoomView> catalog(Actor actor, boolean includeInactive, int page, int size) {
    PageResult.offset(page, size);
    if (includeInactive) actor.requireAdmin();
    var result = repo.page(includeInactive, page, size);
    return new PageResult<>(
        result.items().stream().map(RoomView::from).toList(), result.totalItems(), page, size);
  }

  public RoomView get(UUID id) {
    return RoomView.from(repo.byId(id, false).orElseThrow(() -> AppException.notFound("Room")));
  }

  public RoomView lockActive(UUID id) {
    Room r = repo.byId(id, true).orElseThrow(() -> AppException.notFound("Room"));
    if (!r.isActive()) throw AppException.invalid("The room is inactive");
    return RoomView.from(r);
  }

  @Transactional
  public RoomView create(Actor actor, Input input) {
    actor.requireAdmin();
    repo.lockCatalog();
    int slot = repo.availableSlot();
    if (slot < 1)
      throw AppException.conflict("Room.LimitReached", "At most 10 active rooms are allowed");
    validate(input, null);
    Room r = build(UUID.randomUUID(), slot, input, true, 0, clock.instant());
    repo.insert(r, actor.id());
    audit.record(actor.id(), "room_created", "room", r.id());
    return get(r.id());
  }

  @Transactional
  public RoomView update(Actor actor, UUID id, Input input, boolean active, long version) {
    actor.requireAdmin();
    repo.lockCatalog();
    Room current = repo.byId(id, true).orElseThrow(() -> AppException.notFound("Room"));
    validate(input, id);
    if (!active && usage.stream().anyMatch(u -> u.hasFutureReservations(id, clock.instant())))
      throw AppException.conflict(
          "Room.HasReservations", "Cancel future approved bookings before deactivating this room");
    if (usage.stream()
        .anyMatch(u -> input.capacity() < u.largestFutureMeeting(id, clock.instant())))
      throw AppException.conflict(
          "Room.CapacityInUse", "Future approved bookings exceed the requested capacity");
    int slot = current.slot();
    if (active && !current.isActive()) {
      slot = repo.availableSlot();
      if (slot < 1)
        throw AppException.conflict("Room.LimitReached", "At most 10 active rooms are allowed");
    }
    Room updated = build(id, slot, input, active, version + 1, current.createdAt());
    if (!repo.update(updated, version, actor.id()))
      throw AppException.conflict(
          "Concurrency.StaleVersion", "The room has changed. Reload and retry");
    audit.record(actor.id(), "room_updated", "room", id);
    return get(id);
  }

  private Room build(UUID id, int slot, Input i, boolean active, long version, Instant created) {
    var equipment =
        i.equipment().stream()
            .map(s -> s.trim().toLowerCase(Locale.ROOT))
            .distinct()
            .sorted()
            .toList();
    return new Room(
        id,
        slot,
        i.name().trim(),
        i.description(),
        i.location().trim(),
        i.floor().trim(),
        i.capacity(),
        equipment,
        active,
        version,
        created);
  }

  private void validate(Input i, UUID exclude) {
    if (repo.nameExists(i.name().trim(), exclude))
      throw AppException.conflict("Room.NameExists", "An active room already uses this name");
  }
}
