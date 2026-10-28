package com.dki.meeting.room.infra.db;

import com.dki.meeting.room.application.RoomRepository;
import com.dki.meeting.room.domain.Room;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRoomRepository implements RoomRepository {
  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public JdbcRoomRepository(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public List<Room> list(boolean all) {
    return jdbc.sql("select * from room where (:all or is_active) order by name,id")
        .param("all", all)
        .query(this::map)
        .list();
  }

  public com.dki.meeting.foundation.PageResult<Room> page(boolean all, int page, int size) {
    var items =
        jdbc.sql(
                "select * from room where (:all or is_active) order by name,id limit :size offset :offset")
            .param("all", all)
            .param("size", size)
            .param("offset", com.dki.meeting.foundation.PageResult.offset(page, size))
            .query(this::map)
            .list();
    long total =
        jdbc.sql("select count(*) from room where (:all or is_active)")
            .param("all", all)
            .query(Long.class)
            .single();
    return new com.dki.meeting.foundation.PageResult<>(items, total, page, size);
  }

  public Optional<Room> byId(UUID id, boolean lock) {
    return jdbc.sql("select * from room where id=:id" + (lock ? " for update" : ""))
        .param("id", id)
        .query(this::map)
        .optional();
  }

  public void lockCatalog() {
    jdbc.sql("select id from system_lock where id='room' for update").query(String.class).single();
  }

  public int availableSlot() {
    return jdbc.sql(
            "select coalesce(min(n),0) from generate_series(1,10) as n where not exists(select 1 from room where is_active and slot=n)")
        .query(Integer.class)
        .single();
  }

  public boolean nameExists(String name, UUID exclude) {
    return jdbc.sql(
            "select exists(select 1 from room where is_active and lower(name)=lower(:name) and id<>:exclude)")
        .param("name", name)
        .param("exclude", exclude == null ? new UUID(0, 0) : exclude)
        .query(Boolean.class)
        .single();
  }

  public void insert(Room r, UUID actor) {
    bind(
            jdbc.sql(
                "insert into room(id,slot,name,description,location,floor,capacity,equipment,is_active,created_at,updated_at,created_by,updated_by) values(:id,:slot,:name,:description,:location,:floor,:capacity,cast(:equipment as jsonb),:active,:created,:created,:actor,:actor)"),
            r,
            actor)
        .update();
  }

  public boolean update(Room r, long expected, UUID actor) {
    return bind(
                jdbc.sql(
                    "update room set slot=:slot,name=:name,description=:description,location=:location,floor=:floor,capacity=:capacity,equipment=cast(:equipment as jsonb),is_active=:active,updated_at=now(),updated_by=:actor,version=version+1 where id=:id and version=:expected"),
                r,
                actor)
            .param("expected", expected)
            .update()
        == 1;
  }

  private JdbcClient.StatementSpec bind(JdbcClient.StatementSpec q, Room r, UUID actor) {
    return q.param("id", r.id())
        .param("slot", r.slot())
        .param("name", r.name())
        .param("description", r.description())
        .param("location", r.location())
        .param("floor", r.floor())
        .param("capacity", r.capacity())
        .param("equipment", encode(r.equipment()))
        .param("active", r.isActive())
        .param("actor", actor)
        .param("created", Timestamp.from(r.createdAt()));
  }

  private Room map(ResultSet rs, int n) throws SQLException {
    try {
      return new Room(
          rs.getObject("id", UUID.class),
          rs.getInt("slot"),
          rs.getString("name"),
          rs.getString("description"),
          rs.getString("location"),
          rs.getString("floor"),
          rs.getInt("capacity"),
          json.readValue(rs.getString("equipment"), new TypeReference<List<String>>() {}),
          rs.getBoolean("is_active"),
          rs.getLong("version"),
          rs.getTimestamp("created_at").toInstant());
    } catch (JsonProcessingException ex) {
      throw new SQLException("Invalid stored room equipment", ex);
    }
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Cannot encode room equipment", ex);
    }
  }
}
