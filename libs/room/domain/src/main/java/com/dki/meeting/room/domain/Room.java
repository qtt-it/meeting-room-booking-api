package com.dki.meeting.room.domain;

import java.time.Instant;
import java.util.*;

public record Room(
    UUID id,
    int slot,
    String name,
    String description,
    String location,
    String floor,
    int capacity,
    List<String> equipment,
    boolean isActive,
    long version,
    Instant createdAt) {}
