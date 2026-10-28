package com.dki.meeting.infra.db;

import com.dki.meeting.foundation.AuditTrail;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcAuditTrail implements AuditTrail {
  private final JdbcClient jdbc;

  public JdbcAuditTrail(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void record(UUID actor, String action, String type, UUID id) {
    jdbc.sql(
            "insert into activity_audit(actor_id,action,resource_type,resource_id) values(:actor,:action,:type,:id)")
        .param("actor", actor)
        .param("action", action)
        .param("type", type)
        .param("id", id)
        .update();
  }
}
