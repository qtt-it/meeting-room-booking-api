package com.dki.meeting.foundation;

public class AppException extends RuntimeException {
  private final FailureKind kind;
  private final String code;
  private final Object details;

  public AppException(FailureKind kind, String code, String message) {
    this(kind, code, message, null);
  }

  public AppException(FailureKind kind, String code, String message, Object details) {
    super(message);
    this.kind = kind;
    this.code = code;
    this.details = details;
  }

  public FailureKind kind() {
    return kind;
  }

  public String code() {
    return code;
  }

  public Object details() {
    return details;
  }

  public static AppException invalid(String message) {
    return new AppException(FailureKind.RULE_VIOLATION, "BusinessRule.Violated", message);
  }

  public static AppException notFound(String resource) {
    return new AppException(FailureKind.NOT_FOUND, "Resource.NotFound", resource + " not found");
  }

  public static AppException forbidden() {
    return new AppException(
        FailureKind.FORBIDDEN, "Authorization.Forbidden", "You cannot perform this action");
  }

  public static AppException unauthorized() {
    return new AppException(
        FailureKind.UNAUTHENTICATED,
        "Authentication.InvalidCredentials",
        "Invalid credentials or expired session");
  }

  public static AppException conflict(String code, String message) {
    return new AppException(FailureKind.CONFLICT, code, message);
  }
}
