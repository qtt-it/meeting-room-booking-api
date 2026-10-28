package com.dki.meeting.foundation;

public class AppException extends RuntimeException {
  private final int status;
  private final String code;
  private final Object details;

  public AppException(int status, String code, String message) {
    this(status, code, message, null);
  }

  public AppException(int status, String code, String message, Object details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public Object details() {
    return details;
  }

  public static AppException invalid(String message) {
    return new AppException(422, "BusinessRule.Violated", message);
  }

  public static AppException notFound(String resource) {
    return new AppException(404, "Resource.NotFound", resource + " not found");
  }

  public static AppException forbidden() {
    return new AppException(403, "Authorization.Forbidden", "You cannot perform this action");
  }

  public static AppException unauthorized() {
    return new AppException(
        401, "Authentication.InvalidCredentials", "Invalid credentials or expired session");
  }

  public static AppException conflict(String code, String message) {
    return new AppException(409, code, message);
  }
}
