package com.dki.meeting.booking.application.model;

import java.time.Instant;
import java.util.UUID;

public record BusySlot(UUID roomId, Instant startsAt, Instant endsAt) {}
