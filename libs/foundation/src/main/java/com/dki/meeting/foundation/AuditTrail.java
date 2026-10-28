package com.dki.meeting.foundation;

import java.util.UUID;

public interface AuditTrail {
  void record(UUID actorId, String action, String resourceType, UUID resourceId);
}
