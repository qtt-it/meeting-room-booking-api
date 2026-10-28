package com.dki.meeting.room.application.port.out;

import com.dki.meeting.room.domain.Room;
import java.util.*;

public interface RoomRepository {
  List<Room> list(boolean includeInactive);

  com.dki.meeting.foundation.PageResult<Room> page(boolean includeInactive, int page, int size);

  Optional<Room> byId(UUID id, boolean lock);

  void lockCatalog();

  int availableSlot();

  boolean nameExists(String name, UUID exclude);

  void insert(Room room, UUID actor);

  boolean update(Room room, long expectedVersion, UUID actor);
}
