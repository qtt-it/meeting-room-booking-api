package com.dki.meeting.booking.infra.db;

import com.dki.meeting.room.application.RoomUsage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class BookingRoomUsage implements RoomUsage {
  private final JdbcClient jdbc;

  public BookingRoomUsage(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public boolean hasFutureReservations(UUID room, Instant now) {
    return jdbc.sql(
            "select exists(select 1 from booking_occurrence where room_id=:room and status='approved' and ends_at>:now)")
        .param("room", room)
        .param("now", Timestamp.from(now))
        .query(Boolean.class)
        .single();
  }

  public int largestFutureMeeting(UUID room, Instant now) {
    return jdbc.sql(
            "select coalesce(max(1+(select count(*) from booking_attendee a where a.booking_id=o.booking_id)),0) from booking_occurrence o where room_id=:room and status='approved' and ends_at>:now")
        .param("room", room)
        .param("now", Timestamp.from(now))
        .query(Integer.class)
        .single();
  }
}
