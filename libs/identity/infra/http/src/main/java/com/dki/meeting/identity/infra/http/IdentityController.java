package com.dki.meeting.identity.infra.http;

import com.dki.meeting.foundation.*;
import com.dki.meeting.foundation.http.ApiResponse;
import com.dki.meeting.identity.application.IdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IdentityController {
  private final IdentityService service;

  public IdentityController(IdentityService service) {
    this.service = service;
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
  public ResponseEntity<?> login(@Valid @RequestBody Login r) {
    return tokenResponse(service.login(r.email(), r.password()));
  }

  @PostMapping("/auth/refresh")
  public ResponseEntity<?> refresh(@Valid @RequestBody Refresh r) {
    return tokenResponse(service.refresh(r.refreshToken()));
  }

  @PostMapping("/auth/logout")
  public ApiResponse<?> logout(@Valid @RequestBody Refresh r) {
    service.logout(r.refreshToken());
    return ApiResponse.ok(null, "Logged out");
  }

  @GetMapping("/users/me")
  public ApiResponse<?> me() {
    return ApiResponse.ok(service.me(SecurityActor.current()), "Retrieved successfully");
  }

  @PutMapping("/users/me/password")
  public ApiResponse<?> password(@Valid @RequestBody PasswordChange r) {
    service.changePassword(SecurityActor.current(), r.currentPassword(), r.newPassword());
    return ApiResponse.ok(null, "Password changed. Sign in again");
  }

  @GetMapping("/users")
  public ApiResponse<?> list(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(service.users(SecurityActor.current(), page, pageSize));
  }

  @GetMapping("/users/directory")
  public ApiResponse<?> directory(
      @RequestParam(defaultValue = "") @Size(max = 100) String keyword,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(service.directory(keyword, page, pageSize));
  }

  @PostMapping("/users")
  public ResponseEntity<?> create(@Valid @RequestBody CreateUser r) {
    var u =
        service.create(SecurityActor.current(), r.email(), r.displayName(), r.password(), r.role());
    return ResponseEntity.created(java.net.URI.create("/api/v1/users/" + u.id()))
        .body(ApiResponse.ok(u, "User created"));
  }

  @PutMapping("/users/{id}")
  public ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateUser r) {
    return ApiResponse.ok(
        service.update(
            SecurityActor.current(), id, r.displayName(), r.role(), r.isActive(), r.version()),
        "User updated");
  }

  private ResponseEntity<?> tokenResponse(Object result) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(ApiResponse.ok(result, "Authenticated successfully"));
  }
}
