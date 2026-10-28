package com.dki.meeting.room.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dki.meeting.foundation.*;
import com.dki.meeting.room.application.port.out.RoomRepository;
import com.dki.meeting.room.application.port.out.RoomUsage;
import com.dki.meeting.room.application.service.RoomService;
import com.dki.meeting.room.domain.Room;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RoomServiceTest {
  private final RoomRepository repo = mock(RoomRepository.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);
  private final Actor admin = new Actor(UUID.randomUUID(), "admin");
  private final com.dki.meeting.room.application.model.RoomCommands.Input input =
      new com.dki.meeting.room.application.model.RoomCommands.Input(
          "Room", null, "HQ", "2", 10, List.of("projector"));

  @Test
  void deniesEmployeesBeforeReadingCatalog() {
    var service = new RoomService(repo, clock, List.of(), mock(AuditTrail.class));
    assertThatThrownBy(() -> service.create(new Actor(UUID.randomUUID(), "user"), input))
        .isInstanceOf(AppException.class);
    verifyNoInteractions(repo);
  }

  @Test
  void rejectsEleventhActiveRoom() {
    when(repo.availableSlot()).thenReturn(0);
    var service = new RoomService(repo, clock, List.of(), mock(AuditTrail.class));
    assertThatThrownBy(() -> service.create(admin, input))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).code())
        .isEqualTo("Room.LimitReached");
    verify(repo).lockCatalog();
    verify(repo, never()).insert(any(), any());
  }

  @Test
  void preventsDeactivationWhenTheBookingModuleReportsReservations() {
    UUID id = UUID.randomUUID();
    when(repo.byId(id, true))
        .thenReturn(
            Optional.of(
                new Room(id, 1, "Room", null, "HQ", "2", 10, List.of(), true, 0, clock.instant())));
    var usage = mock(RoomUsage.class);
    when(usage.hasFutureReservations(id, clock.instant())).thenReturn(true);
    assertThatThrownBy(
            () ->
                new RoomService(repo, clock, List.of(usage), mock(AuditTrail.class))
                    .update(admin, id, input, false, 0))
        .isInstanceOf(AppException.class);
    verify(repo, never()).update(any(), anyLong(), any());
  }
}
