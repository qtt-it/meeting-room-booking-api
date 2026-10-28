package com.dki.meeting.identity.application.service;

import com.dki.meeting.foundation.*;
import com.dki.meeting.identity.application.RefreshReuseException;
import com.dki.meeting.identity.application.model.IdentityModels.*;
import com.dki.meeting.identity.application.port.out.*;
import com.dki.meeting.identity.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

public class IdentityService {
  private final IdentityRepository repo;
  private final PasswordHasher passwords;
  private final AccessTokens tokens;
  private final Clock clock;
  private final AuditTrail audit;
  private final String dummyHash;
  private final SecureRandom random = new SecureRandom();

  public IdentityService(
      IdentityRepository repo,
      PasswordHasher passwords,
      AccessTokens tokens,
      Clock clock,
      AuditTrail audit) {
    this.repo = repo;
    this.passwords = passwords;
    this.tokens = tokens;
    this.clock = clock;
    this.audit = audit;
    this.dummyHash = passwords.hash(UUID.randomUUID().toString());
  }

  public Tokens login(String email, String password) {
    var user = repo.byEmail(normalizeEmail(email));
    boolean valid =
        passwords.matches(password, user.map(UserAccount::passwordHash).orElse(dummyHash));
    if (!valid || user.isEmpty() || !user.get().isActive()) throw AppException.unauthorized();
    audit.record(user.get().id(), "session_created", "user", user.get().id());
    UUID family = UUID.randomUUID();
    repo.createFamily(family, user.get().id());
    return issue(user.get(), family);
  }

  public Tokens refresh(String raw) {
    String hash = hashToken(raw);
    RefreshSession hint = repo.session(hash, false).orElseThrow(AppException::unauthorized);
    if (!repo.lockFamily(hint.familyId())) throw AppException.unauthorized();
    RefreshSession session = repo.session(hash, true).orElseThrow(AppException::unauthorized);
    if (session.revokedAt() != null) {
      repo.revokeFamily(session.familyId(), clock.instant());
      audit.record(session.userId(), "refresh_reuse_detected", "session", session.familyId());
      throw new RefreshReuseException();
    }
    if (!session.expiresAt().isAfter(clock.instant())) throw AppException.unauthorized();
    UserAccount user = active(session.userId());
    if (session.securityVersion() != user.securityVersion()) throw AppException.unauthorized();
    repo.revokeSession(session.id(), clock.instant());
    audit.record(user.id(), "session_refreshed", "session", session.familyId());
    return issue(user, session.familyId());
  }

  public void logout(String raw) {
    repo.session(hashToken(raw), false)
        .ifPresent(
            s -> {
              repo.lockFamily(s.familyId());
              repo.revokeFamily(s.familyId(), clock.instant());
              audit.record(s.userId(), "session_logged_out", "session", s.familyId());
            });
  }

  public UserAccount active(UUID id) {
    var u = repo.byId(id).orElseThrow(AppException::unauthorized);
    if (!u.isActive()) throw AppException.unauthorized();
    return u;
  }

  public UserView me(Actor actor) {
    return UserView.from(active(actor.id()));
  }

  public PageResult<UserView> users(Actor actor, int page, int size) {
    actor.requireAdmin();
    PageResult.offset(page, size);
    var list = repo.list(page, size);
    return new PageResult<>(
        list.items().stream().map(UserView::from).toList(), list.totalItems(), page, size);
  }

  public PageResult<UserView> directory(String keyword, int page, int size) {
    String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    if (needle.length() > 100) throw AppException.invalid("The search keyword is too long");
    PageResult.offset(page, size);
    var found = repo.directory(needle, page, size);
    return new PageResult<>(
        found.items().stream().map(UserView::from).toList(), found.totalItems(), page, size);
  }

  public UserView create(Actor actor, String email, String name, String password, String role) {
    actor.requireAdmin();
    repo.lockCatalog();
    return insert(email, name, password, role, actor.id());
  }

  public void bootstrap(String email, String password) {
    if (email == null || email.isBlank() || password == null || password.isBlank()) return;
    repo.lockCatalog();
    if (repo.count() == 0) insert(email, "Administrator", password, "admin", null);
  }

  private UserView insert(String email, String name, String password, String role, UUID actor) {
    checkRole(role);
    String normalized = normalizeEmail(email);
    if (repo.byEmail(normalized).isPresent())
      throw AppException.conflict("Identity.EmailExists", "The email address is already in use");
    var u =
        new UserAccount(
            UUID.randomUUID(),
            normalized,
            name.trim(),
            passwords.hash(password),
            role,
            true,
            0,
            0,
            clock.instant());
    repo.insert(u, actor == null ? u.id() : actor);
    audit.record(actor == null ? u.id() : actor, "user_created", "user", u.id());
    return UserView.from(u);
  }

  public UserView update(
      Actor actor, UUID id, String name, String role, boolean active, long version) {
    actor.requireAdmin();
    checkRole(role);
    repo.lockCatalog();
    UserAccount current = repo.byId(id).orElseThrow(() -> AppException.notFound("User"));
    if (current.isActive()
        && "admin".equals(current.role())
        && (!active || !"admin".equals(role))
        && repo.activeAdminCount() <= 1)
      throw AppException.invalid("The last active administrator cannot be removed");
    if (!repo.update(id, name.trim(), role, active, version, actor.id()))
      throw AppException.conflict(
          "Concurrency.StaleVersion", "The user has changed. Reload and retry");
    if (!active) repo.revokeUserSessions(id, clock.instant());
    audit.record(actor.id(), "user_updated", "user", id);
    return UserView.from(repo.byId(id).orElseThrow());
  }

  public void changePassword(Actor actor, String current, String replacement) {
    UserAccount user = active(actor.id());
    if (!passwords.matches(current, user.passwordHash())) throw AppException.unauthorized();
    if (!repo.changePassword(
        user.id(), passwords.hash(replacement), actor.id(), user.securityVersion()))
      throw AppException.conflict(
          "Concurrency.StaleVersion", "The account has changed. Sign in and retry");
    repo.revokeUserSessions(user.id(), clock.instant());
    audit.record(actor.id(), "password_changed", "user", actor.id());
  }

  public void validateInvitees(List<UUID> ids) {
    if (repo.countActive(ids) != ids.size())
      throw AppException.invalid("Every attendee must be an active employee");
  }

  private Tokens issue(UserAccount user, UUID family) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    repo.insertSession(
        new RefreshSession(
            UUID.randomUUID(),
            user.id(),
            family,
            hashToken(raw),
            user.securityVersion(),
            clock.instant().plus(Duration.ofDays(7)),
            null));
    return new Tokens(
        tokens.issue(user), raw, "Bearer", tokens.expiresInSeconds(), UserView.from(user));
  }

  private void checkRole(String role) {
    if (!Set.of("user", "admin").contains(role))
      throw AppException.invalid("The role must be user or admin");
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String hashToken(String token) {
    if (token == null || token.length() < 20 || token.length() > 256)
      throw AppException.unauthorized();
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }
}
