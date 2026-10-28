package com.dki.meeting.identity.infra.http;

import com.dki.meeting.foundation.*;
import com.dki.meeting.foundation.http.AdminOnly;
import com.dki.meeting.foundation.http.ApiResponse;
import com.dki.meeting.foundation.http.CurrentOperation;
import com.dki.meeting.identity.application.model.*;
import com.dki.meeting.identity.application.port.in.IdentityHandlers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IdentityController {
  private final IdentityHandlers handlers;

  public IdentityController(IdentityHandlers handlers) {
    this.handlers = handlers;
  }

  public record Login(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {
    @Override
    public String toString() {
      return "Login[redacted]";
    }
  }

  public record Refresh(@NotBlank @Size(max = 256) String refreshToken) {
    @Override
    public String toString() {
      return "Refresh[redacted]";
    }
  }

  public record CreateUser(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 100) String displayName,
      @NotBlank @Size(min = 12, max = 72) String password,
      @Pattern(regexp = "user|admin") @NotNull String role) {
    @Override
    public String toString() {
      return "CreateUser[redacted]";
    }
  }

  public record UpdateUser(
      @NotBlank @Size(max = 100) String displayName,
      @NotNull @Pattern(regexp = "user|admin") String role,
      @NotNull Boolean isActive,
      @NotNull @PositiveOrZero Long version) {}

  public record PasswordChange(
      @NotBlank @Size(max = 72) String currentPassword,
      @NotBlank @Size(min = 12, max = 72) String newPassword) {
    @Override
    public String toString() {
      return "PasswordChange[redacted]";
    }
  }

  @PostMapping("/auth/login")
  public ResponseEntity<?> login(
      @CurrentOperation OperationContext context, @Valid @RequestBody Login r) {
    return tokenResponse(
        handlers.login().execute(context, new IdentityCommands.Login(r.email(), r.password())));
  }

  @PostMapping("/auth/refresh")
  public ResponseEntity<?> refresh(
      @CurrentOperation OperationContext context, @Valid @RequestBody Refresh r) {
    return tokenResponse(
        handlers.refresh().execute(context, new IdentityCommands.Refresh(r.refreshToken())));
  }

  @PostMapping("/auth/logout")
  public ApiResponse<?> logout(
      @CurrentOperation OperationContext context, @Valid @RequestBody Refresh r) {
    handlers.logout().execute(context, new IdentityCommands.Refresh(r.refreshToken()));
    return ApiResponse.ok(null, "Logged out");
  }

  @GetMapping("/users/me")
  public ApiResponse<?> me(@CurrentOperation OperationContext context) {
    return ApiResponse.ok(handlers.me().execute(context, null), "Retrieved successfully");
  }

  @PutMapping("/users/me/password")
  public ApiResponse<?> password(
      @CurrentOperation OperationContext context, @Valid @RequestBody PasswordChange r) {
    handlers
        .changePassword()
        .execute(
            context, new IdentityCommands.PasswordChange(r.currentPassword(), r.newPassword()));
    return ApiResponse.ok(null, "Password changed. Sign in again");
  }

  @AdminOnly
  @GetMapping("/users")
  public ApiResponse<?> list(
      @CurrentOperation OperationContext context,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(
        handlers.users().execute(context, new IdentityCommands.Page(page, pageSize)));
  }

  @GetMapping("/users/directory")
  public ApiResponse<?> directory(
      @CurrentOperation OperationContext context,
      @RequestParam(defaultValue = "") @Size(max = 100) String keyword,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(
        handlers
            .directory()
            .execute(context, new IdentityCommands.Directory(keyword, page, pageSize)));
  }

  @AdminOnly
  @PostMapping("/users")
  public ResponseEntity<?> create(
      @CurrentOperation OperationContext context, @Valid @RequestBody CreateUser r) {
    var u =
        handlers
            .create()
            .execute(
                context,
                new IdentityCommands.Create(r.email(), r.displayName(), r.password(), r.role()));
    return ResponseEntity.created(java.net.URI.create("/api/v1/users/" + u.id()))
        .body(ApiResponse.ok(u, "User created"));
  }

  @AdminOnly
  @PutMapping("/users/{id}")
  public ApiResponse<?> update(
      @CurrentOperation OperationContext context,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateUser r) {
    return ApiResponse.ok(
        handlers
            .update()
            .execute(
                context,
                new IdentityCommands.Update(
                    id, r.displayName(), r.role(), r.isActive(), r.version())),
        "User updated");
  }

  private ResponseEntity<?> tokenResponse(Object result) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(ApiResponse.ok(result, "Authenticated successfully"));
  }
}
