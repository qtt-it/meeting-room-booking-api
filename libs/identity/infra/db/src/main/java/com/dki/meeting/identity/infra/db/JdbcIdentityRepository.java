package com.dki.meeting.identity.infra.db;

import com.dki.meeting.foundation.PageResult;
import com.dki.meeting.identity.application.port.out.IdentityRepository;
import com.dki.meeting.identity.domain.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIdentityRepository implements IdentityRepository {
  private final JdbcClient jdbc;

  public JdbcIdentityRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<UserAccount> byEmail(String email) {
    return jdbc.sql("select * from user_account where email=:email")
        .param("email", email)
        .query(this::user)
        .optional();
  }

  public Optional<UserAccount> byId(UUID id) {
    return jdbc.sql("select * from user_account where id=:id")
        .param("id", id)
        .query(this::user)
        .optional();
  }

  public int countActive(List<UUID> ids) {
    if (ids.isEmpty()) return 0;
    return jdbc.sql("select count(*) from user_account where is_active and id in (:ids)")
        .param("ids", ids)
        .query(Integer.class)
        .single();
  }

  public void lockCatalog() {
    jdbc.sql("select id from system_lock where id='identity' for update")
        .query(String.class)
        .single();
  }

  public long activeAdminCount() {
    return jdbc.sql("select count(*) from user_account where role='admin' and is_active")
        .query(Long.class)
        .single();
  }

  public long count() {
    return jdbc.sql("select count(*) from user_account").query(Long.class).single();
  }

  public PageResult<UserAccount> list(int page, int size) {
    var items =
        jdbc.sql("select * from user_account order by display_name,id limit :size offset :offset")
            .param("size", size)
            .param("offset", PageResult.offset(page, size))
            .query(this::user)
            .list();
    return new PageResult<>(items, count(), page, size);
  }

  public void insert(UserAccount u, UUID actor) {
    jdbc.sql(
            "insert into user_account(id,email,display_name,password_hash,role,created_at,updated_at,created_by,updated_by) values(:id,:email,:name,:password,:role,:now,:now,:actor,:actor)")
        .param("id", u.id())
        .param("email", u.email())
        .param("name", u.displayName())
        .param("password", u.passwordHash())
        .param("role", u.role())
        .param("now", Timestamp.from(u.createdAt()))
        .param("actor", actor)
        .update();
  }

  public PageResult<UserAccount> directory(String keyword, int page, int size) {
    String filter =
        " from user_account where is_active and (position(:keyword in lower(display_name))>0 or position(:keyword in email)>0)";
    var items =
        jdbc.sql("select *" + filter + " order by display_name,id limit :size offset :offset")
            .param("keyword", keyword)
            .param("size", size)
            .param("offset", PageResult.offset(page, size))
            .query(this::user)
            .list();
    long total =
        jdbc.sql("select count(*)" + filter).param("keyword", keyword).query(Long.class).single();
    return new PageResult<>(items, total, page, size);
  }

  public boolean update(
      UUID id, String name, String role, boolean active, long version, UUID actor) {
    return jdbc.sql(
                "update user_account set display_name=:name,role=:role,is_active=:active,version=version+1,updated_at=now(),updated_by=:actor,security_version=security_version+case when is_active<>:active then 1 else 0 end where id=:id and version=:version")
            .param("id", id)
            .param("name", name)
            .param("role", role)
            .param("active", active)
            .param("actor", actor)
            .param("version", version)
            .update()
        == 1;
  }

  public boolean changePassword(UUID id, String hash, UUID actor, long expected) {
    return jdbc.sql(
                "update user_account set password_hash=:hash,security_version=security_version+1,version=version+1,updated_at=now(),updated_by=:actor where id=:id and security_version=:expected")
            .param("id", id)
            .param("hash", hash)
            .param("actor", actor)
            .param("expected", expected)
            .update()
        == 1;
  }

  public void insertSession(RefreshSession s) {
    jdbc.sql(
            "insert into refresh_session(id,user_account_id,family_id,token_hash,security_version,expires_at) values(:id,:user,:family,:hash,:sv,:expiry)")
        .param("id", s.id())
        .param("user", s.userId())
        .param("family", s.familyId())
        .param("hash", s.tokenHash())
        .param("sv", s.securityVersion())
        .param("expiry", Timestamp.from(s.expiresAt()))
        .update();
  }

  public void createFamily(UUID id, UUID userId) {
    jdbc.sql("insert into refresh_family(id,user_account_id) values(:id,:user)")
        .param("id", id)
        .param("user", userId)
        .update();
  }

  public boolean lockFamily(UUID id) {
    return jdbc.sql("select revoked_at is null from refresh_family where id=:id for update")
        .param("id", id)
        .query(Boolean.class)
        .optional()
        .orElse(false);
  }

  public Optional<RefreshSession> session(String hash, boolean lock) {
    return jdbc.sql(
            "select * from refresh_session where token_hash=:hash" + (lock ? " for update" : ""))
        .param("hash", hash)
        .query(
            (rs, n) ->
                new RefreshSession(
                    rs.getObject("id", UUID.class),
                    rs.getObject("user_account_id", UUID.class),
                    rs.getObject("family_id", UUID.class),
                    rs.getString("token_hash"),
                    rs.getLong("security_version"),
                    rs.getTimestamp("expires_at").toInstant(),
                    rs.getTimestamp("revoked_at") == null
                        ? null
                        : rs.getTimestamp("revoked_at").toInstant()))
        .optional();
  }

  public void revokeSession(UUID id, Instant now) {
    jdbc.sql("update refresh_session set revoked_at=:now where id=:id and revoked_at is null")
        .param("id", id)
        .param("now", Timestamp.from(now))
        .update();
  }

  public void revokeFamily(UUID family, Instant now) {
    jdbc.sql("update refresh_family set revoked_at=coalesce(revoked_at,:now) where id=:id")
        .param("id", family)
        .param("now", Timestamp.from(now))
        .update();
    jdbc.sql(
            "update refresh_session set revoked_at=coalesce(revoked_at,:now) where family_id=:family")
        .param("family", family)
        .param("now", Timestamp.from(now))
        .update();
  }

  public void revokeUserSessions(UUID user, Instant now) {
    jdbc.sql(
            "update refresh_session set revoked_at=coalesce(revoked_at,:now) where user_account_id=:user")
        .param("user", user)
        .param("now", Timestamp.from(now))
        .update();
  }

  private UserAccount user(ResultSet rs, int n) throws SQLException {
    return new UserAccount(
        rs.getObject("id", UUID.class),
        rs.getString("email"),
        rs.getString("display_name"),
        rs.getString("password_hash"),
        rs.getString("role"),
        rs.getBoolean("is_active"),
        rs.getLong("version"),
        rs.getLong("security_version"),
        rs.getTimestamp("created_at").toInstant());
  }
}
