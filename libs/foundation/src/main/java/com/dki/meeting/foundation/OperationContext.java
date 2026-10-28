package com.dki.meeting.foundation;

public record OperationContext(Actor actor, String requestId) {
  public Actor requireActor() {
    if (actor == null) throw AppException.unauthorized();
    return actor;
  }

  public static OperationContext system() {
    return new OperationContext(new Actor(new java.util.UUID(0, 0), "system"), "system-bootstrap");
  }
}
