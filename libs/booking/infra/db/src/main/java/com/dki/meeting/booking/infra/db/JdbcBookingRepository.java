package com.dki.meeting.booking.infra.db;

import com.dki.meeting.booking.application.BookingRepository;
import com.dki.meeting.booking.domain.*;
import com.dki.meeting.booking.domain.Booking.*;
import com.dki.meeting.foundation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBookingRepository implements BookingRepository {
  private static final UUID ZERO = new UUID(0, 0);
  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public JdbcBookingRepository(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Optional<Booking> byId(UUID id, boolean lock) {
    return jdbc.sql("select * from booking where id=:id" + (lock ? " for update" : ""))
        .param("id", id)
        .query(this::map)
        .optional();
  }

  public void insert(Booking b, UUID actor) {
    jdbc.sql(
            "insert into booking(id,room_id,organizer_id,title,description,status,recurrence,created_at,updated_at,created_by,updated_by) values(:id,:room,:organizer,:title,:description,'pending',cast(:recurrence as jsonb),:now,:now,:actor,:actor)")
        .param("id", b.id())
        .param("room", b.roomId())
        .param("organizer", b.organizerId())
        .param("title", b.title())
        .param("description", b.description())
        .param("recurrence", encode(b.recurrence()))
        .param("now", Timestamp.from(b.createdAt()))
        .param("actor", actor)
        .update();
    insertChildren(b);
  }

  public void replace(Booking b, UUID actor) {
    jdbc.sql("delete from booking_occurrence where booking_id=:id").param("id", b.id()).update();
    jdbc.sql("delete from booking_attendee where booking_id=:id").param("id", b.id()).update();
    jdbc.sql(
            "update booking set room_id=:room,title=:title,description=:description,status='pending',recurrence=cast(:recurrence as jsonb),version=version+1,decided_by=null,decision_reason=null,updated_at=now(),updated_by=:actor where id=:id")
        .param("id", b.id())
        .param("room", b.roomId())
        .param("title", b.title())
        .param("description", b.description())
        .param("recurrence", encode(b.recurrence()))
        .param("actor", actor)
        .update();
    insertChildren(b);
  }

  private void insertChildren(Booking b) {
    for (var o : b.occurrences())
      jdbc.sql(
              "insert into booking_occurrence(id,booking_id,room_id,starts_at,ends_at,status) values(:id,:booking,:room,:start,:end,'pending')")
          .param("id", o.id())
          .param("booking", b.id())
          .param("room", b.roomId())
          .param("start", Timestamp.from(o.startsAt()))
          .param("end", Timestamp.from(o.endsAt()))
          .update();
    for (var a : b.attendees())
      jdbc.sql(
              "insert into booking_attendee(booking_id,user_account_id,response) values(:id,:user,'pending')")
          .param("id", b.id())
          .param("user", a.userId())
          .update();
  }

  public void changeStatus(UUID id, String status, UUID actor, String reason, Instant now) {
    jdbc.sql(
            "update booking set status=:status,version=version+1,decided_by=:actor,decision_reason=:reason,updated_at=:now,updated_by=:actor where id=:id")
        .param("id", id)
        .param("status", status)
        .param("actor", actor)
        .param("reason", reason)
        .param("now", Timestamp.from(now))
        .update();
    jdbc.sql(
            "update booking_occurrence set status=:status where booking_id=:id and (:status<>'cancelled' or starts_at>:now)")
        .param("id", id)
        .param("status", status)
        .param("now", Timestamp.from(now))
        .update();
  }

  public PageResult<Booking> list(Actor actor, String scope, String status, int page, int size) {
    String filter = " where (:status='' or b.status=:status)";
    if (scope.equals("mine")) filter += " and b.organizer_id=:actor";
    if (scope.equals("invited"))
      filter +=
          " and exists(select 1 from booking_attendee a where a.booking_id=b.id and a.user_account_id=:actor)";
    var q =
        jdbc.sql(
                "select b.* from booking b"
                    + filter
                    + " order by b.created_at desc,b.id limit :size offset :offset")
            .param("status", status)
            .param("actor", actor.id())
            .param("size", size)
            .param("offset", PageResult.offset(page, size));
    var items = hydrate(q.query(this::header).list());
    long total =
        jdbc.sql("select count(*) from booking b" + filter)
            .param("status", status)
            .param("actor", actor.id())
            .query(Long.class)
            .single();
    return new PageResult<>(items, total, page, size);
  }

  public List<TimeWindow> occupied(UUID room, Instant from, Instant to, UUID exclude) {
    return jdbc.sql(
            "select starts_at,ends_at from booking_occurrence where room_id=:room and status='approved' and booking_id<>:exclude and starts_at<:to and ends_at>:from order by starts_at")
        .param("room", room)
        .param("exclude", exclude == null ? ZERO : exclude)
        .param("from", Timestamp.from(from))
        .param("to", Timestamp.from(to))
        .query(
            (rs, n) ->
                new TimeWindow(rs.getTimestamp(1).toInstant(), rs.getTimestamp(2).toInstant()))
        .list();
  }

  public PageResult<BusySlot> calendar(UUID room, Instant from, Instant to, int page, int size) {
    String filter =
        " from booking_occurrence where room_id=:room and status='approved' and starts_at<:to and ends_at>:from";
    var data =
        jdbc.sql(
                "select room_id,starts_at,ends_at"
                    + filter
                    + " order by starts_at,id limit :size offset :offset")
            .param("room", room)
            .param("from", Timestamp.from(from))
            .param("to", Timestamp.from(to))
            .param("size", size)
            .param("offset", PageResult.offset(page, size))
            .query(
                (rs, n) ->
                    new BusySlot(
                        rs.getObject(1, UUID.class),
                        rs.getTimestamp(2).toInstant(),
                        rs.getTimestamp(3).toInstant()))
            .list();
    long total =
        jdbc.sql("select count(*)" + filter)
            .param("room", room)
            .param("from", Timestamp.from(from))
            .param("to", Timestamp.from(to))
            .query(Long.class)
            .single();
    return new PageResult<>(data, total, page, size);
  }

  public boolean respond(UUID booking, UUID user, String response) {
    return jdbc.sql(
                "update booking_attendee set response=:response,responded_at=now() where booking_id=:booking and user_account_id=:user")
            .param("booking", booking)
            .param("user", user)
            .param("response", response)
            .update()
        == 1;
  }

  public void audit(UUID booking, UUID actor, String action, String details) {
    jdbc.sql(
            "insert into booking_audit(booking_id,actor_id,action,details) values(:booking,:actor,:action,:details)")
        .param("booking", booking)
        .param("actor", actor)
        .param("action", action)
        .param("details", details)
        .update();
  }

  private Booking map(ResultSet rs, int n) throws SQLException {
    return hydrate(List.of(header(rs, n))).get(0);
  }

  private List<Booking> hydrate(List<Booking> headers) {
    if (headers.isEmpty()) return headers;
    var ids = headers.stream().map(Booking::id).toList();
    Map<UUID, List<Occurrence>> occurrences = new HashMap<>();
    jdbc.sql("select * from booking_occurrence where booking_id in (:ids) order by starts_at,id")
        .param("ids", ids)
        .query(
            (r, i) -> {
              occurrences
                  .computeIfAbsent(r.getObject("booking_id", UUID.class), k -> new ArrayList<>())
                  .add(
                      new Occurrence(
                          r.getObject("id", UUID.class),
                          r.getTimestamp("starts_at").toInstant(),
                          r.getTimestamp("ends_at").toInstant(),
                          r.getString("status")));
              return r.getObject("booking_id", UUID.class);
            })
        .list();
    Map<UUID, List<Attendee>> attendees = new HashMap<>();
    jdbc.sql("select * from booking_attendee where booking_id in (:ids) order by user_account_id")
        .param("ids", ids)
        .query(
            (r, i) -> {
              attendees
                  .computeIfAbsent(r.getObject("booking_id", UUID.class), k -> new ArrayList<>())
                  .add(
                      new Attendee(
                          r.getObject("user_account_id", UUID.class), r.getString("response")));
              return r.getObject("booking_id", UUID.class);
            })
        .list();
    return headers.stream()
        .map(
            b ->
                new Booking(
                    b.id(),
                    b.roomId(),
                    b.organizerId(),
                    b.title(),
                    b.description(),
                    b.status(),
                    b.recurrence(),
                    b.version(),
                    b.createdAt(),
                    b.updatedAt(),
                    b.decidedBy(),
                    b.decisionReason(),
                    List.copyOf(occurrences.getOrDefault(b.id(), List.of())),
                    List.copyOf(attendees.getOrDefault(b.id(), List.of()))))
        .toList();
  }

  private Booking header(ResultSet rs, int n) throws SQLException {
    UUID id = rs.getObject("id", UUID.class);
    try {
      return new Booking(
          id,
          rs.getObject("room_id", UUID.class),
          rs.getObject("organizer_id", UUID.class),
          rs.getString("title"),
          rs.getString("description"),
          rs.getString("status"),
          json.readValue(rs.getString("recurrence"), Recurrence.class),
          rs.getLong("version"),
          rs.getTimestamp("created_at").toInstant(),
          rs.getTimestamp("updated_at").toInstant(),
          rs.getObject("decided_by", UUID.class),
          rs.getString("decision_reason"),
          List.of(),
          List.of());
    } catch (JsonProcessingException ex) {
      throw new SQLException("Invalid stored recurrence", ex);
    }
  }

  private String encode(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Cannot encode recurrence", ex);
    }
  }
}
