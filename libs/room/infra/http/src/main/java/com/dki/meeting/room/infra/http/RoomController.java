package com.dki.meeting.room.infra.http;

import com.dki.meeting.foundation.*;
import com.dki.meeting.foundation.http.ApiResponse;
import com.dki.meeting.room.application.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
  private final RoomService service;

  public RoomController(RoomService service) {
    this.service = service;
  }

  public record RoomInput(
      @NotBlank @Size(max = 100) String name,
      @Size(max = 2000) String description,
      @NotBlank @Size(max = 100) String location,
      @NotBlank @Size(max = 30) String floor,
      @Min(1) @Max(200) int capacity,
      @NotNull @Size(max = 30) List<@NotBlank @Size(max = 50) String> equipment,
      Boolean isActive,
      @PositiveOrZero Long version) {
    RoomService.Input input() {
      return new RoomService.Input(name, description, location, floor, capacity, equipment);
    }
  }

  @GetMapping
  public ApiResponse<?> list(
      @RequestParam(defaultValue = "false") boolean includeInactive,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(
        service.catalog(SecurityActor.current(), includeInactive, page, pageSize));
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable UUID id) {
    var r = service.get(id);
    if (!r.isActive()) SecurityActor.current().requireAdmin();
    return ApiResponse.ok(r, "Retrieved successfully");
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody RoomInput r) {
    var room = service.create(SecurityActor.current(), r.input());
    return ResponseEntity.created(java.net.URI.create("/api/v1/rooms/" + room.id()))
        .body(ApiResponse.ok(room, "Room created"));
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody RoomInput r) {
    if (r.version() == null || r.isActive() == null)
      throw new AppException(400, "Validation.InvalidRequest", "version and isActive are required");
    return ApiResponse.ok(
        service.update(SecurityActor.current(), id, r.input(), r.isActive(), r.version()),
        "Room updated");
  }
}
