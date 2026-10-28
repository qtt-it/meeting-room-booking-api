package com.dki.meeting.room.infra.http;

import com.dki.meeting.foundation.*;
import com.dki.meeting.foundation.http.AdminOnly;
import com.dki.meeting.foundation.http.ApiResponse;
import com.dki.meeting.foundation.http.CurrentOperation;
import com.dki.meeting.room.application.model.*;
import com.dki.meeting.room.application.port.in.RoomHandlers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
  private final RoomHandlers handlers;

  public RoomController(RoomHandlers handlers) {
    this.handlers = handlers;
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
    com.dki.meeting.room.application.model.RoomCommands.Input input() {
      return new com.dki.meeting.room.application.model.RoomCommands.Input(
          name, description, location, floor, capacity, equipment);
    }
  }

  @GetMapping
  public ApiResponse<?> list(
      @CurrentOperation OperationContext context,
      @RequestParam(defaultValue = "false") boolean includeInactive,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.page(
        handlers
            .catalog()
            .execute(context, new RoomCommands.Catalog(includeInactive, page, pageSize)));
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@CurrentOperation OperationContext context, @PathVariable UUID id) {
    var r = handlers.get().execute(context, id);
    return ApiResponse.ok(r, "Retrieved successfully");
  }

  @AdminOnly
  @PostMapping
  public ResponseEntity<?> create(
      @CurrentOperation OperationContext context, @Valid @RequestBody RoomInput r) {
    var room = handlers.create().execute(context, r.input());
    return ResponseEntity.created(java.net.URI.create("/api/v1/rooms/" + room.id()))
        .body(ApiResponse.ok(room, "Room created"));
  }

  @AdminOnly
  @PutMapping("/{id}")
  public ApiResponse<?> update(
      @CurrentOperation OperationContext context,
      @PathVariable UUID id,
      @Valid @RequestBody RoomInput r) {
    if (r.version() == null || r.isActive() == null)
      throw new AppException(
          FailureKind.INVALID_REQUEST,
          "Validation.InvalidRequest",
          "version and isActive are required");
    return ApiResponse.ok(
        handlers
            .update()
            .execute(context, new RoomCommands.Update(id, r.input(), r.isActive(), r.version())),
        "Room updated");
  }
}
