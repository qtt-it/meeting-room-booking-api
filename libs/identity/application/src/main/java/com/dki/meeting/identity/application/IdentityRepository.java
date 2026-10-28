package com.dki.meeting.identity.application;

import com.dki.meeting.foundation.PageResult;
import com.dki.meeting.identity.domain.*;
import java.time.Instant;
import java.util.*;

public interface IdentityRepository {
  Optional<UserAccount> byEmail(String email);

  Optional<UserAccount> byId(UUID id);

  void lockCatalog();

  long activeAdminCount();

  long count();

  int countActive(List<UUID> ids);

  PageResult<UserAccount> list(int page, int pageSize);

  PageResult<UserAccount> directory(String keyword, int page, int pageSize);

  void insert(UserAccount user, UUID actorId);

  boolean update(
      UUID id, String displayName, String role, boolean isActive, long version, UUID actorId);

  boolean changePassword(UUID id, String hash, UUID actorId, long expectedSecurityVersion);

  void insertSession(RefreshSession session);

  Optional<RefreshSession> session(String tokenHash, boolean lock);

  void createFamily(UUID id, UUID userId);

  boolean lockFamily(UUID id);

  void revokeSession(UUID id, Instant now);

  void revokeFamily(UUID familyId, Instant now);

  void revokeUserSessions(UUID userId, Instant now);
}
