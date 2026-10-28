package com.dki.meeting.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
    UUID id,
    UUID userId,
    UUID familyId,
    String tokenHash,
    long securityVersion,
    Instant expiresAt,
    Instant revokedAt) {}
