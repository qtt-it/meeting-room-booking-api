package com.dki.meeting.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record UserAccount(
    UUID id,
    String email,
    String displayName,
    String passwordHash,
    String role,
    boolean isActive,
    long version,
    long securityVersion,
    Instant createdAt) {}
