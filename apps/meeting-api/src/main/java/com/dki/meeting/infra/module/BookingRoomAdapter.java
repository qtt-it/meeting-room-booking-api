package com.dki.meeting.infra.module;

import com.dki.meeting.booking.application.model.RoomSummary;
import com.dki.meeting.booking.application.port.out.BookingRooms;
import com.dki.meeting.room.application.RoomView;
import com.dki.meeting.room.application.service.RoomService;
import java.util.*;

public final class BookingRoomAdapter implements BookingRooms {
  private final RoomService rooms;

  public BookingRoomAdapter(RoomService rooms) {
    this.rooms = rooms;
  }

  public RoomSummary lockActive(UUID id) {
    return map(rooms.lockActive(id));
  }

  public RoomSummary find(UUID id) {
    return map(rooms.get(id));
  }

  public List<RoomSummary> activeRooms() {
    return rooms.activeRooms().stream().map(this::map).toList();
  }

  private RoomSummary map(RoomView r) {
    return new RoomSummary(
        r.id(),
        r.name(),
        r.description(),
        r.location(),
        r.floor(),
        r.capacity(),
        r.equipment(),
        r.isActive(),
        r.version(),
        r.createdAt());
  }
}
