package com.dki.meeting.booking.domain;

import static org.assertj.core.api.Assertions.*;

import com.dki.meeting.foundation.AppException;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeetingPolicyTest {
  private final MeetingPolicy policy = new MeetingPolicy();
  private final Instant now = Instant.parse("2026-10-28T00:00:00Z");

  private TimeWindow window(String start, String end) {
    return new TimeWindow(Instant.parse(start), Instant.parse(end));
  }

  @Test
  void adjacentMeetingsDoNotOverlap() {
    var a = window("2026-10-29T02:00:00Z", "2026-10-29T03:00:00Z");
    var b = window("2026-10-29T03:00:00Z", "2026-10-29T04:00:00Z");
    assertThat(a.overlaps(b)).isFalse();
    assertThat(b.overlaps(a)).isFalse();
  }

  @Test
  void nestedMeetingsOverlap() {
    var a = window("2026-10-29T02:00:00Z", "2026-10-29T05:00:00Z");
    var b = window("2026-10-29T03:00:00Z", "2026-10-29T04:00:00Z");
    assertThat(a.overlaps(b)).isTrue();
  }

  @Test
  void permitsFullBusinessDay() {
    assertThatCode(
            () ->
                policy.validate(window("2026-10-31T02:00:00Z", "2026-10-31T11:00:00Z"), now, true))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsSunday() {
    assertThatThrownBy(
            () ->
                policy.validate(window("2026-11-01T02:00:00Z", "2026-11-01T03:00:00Z"), now, true))
        .isInstanceOf(AppException.class);
  }

  @Test
  void rejectsOutsideHours() {
    assertThatThrownBy(
            () ->
                policy.validate(window("2026-10-29T01:59:00Z", "2026-10-29T03:00:00Z"), now, true))
        .isInstanceOf(AppException.class);
  }

  @Test
  void rejectsAfterClosing() {
    assertThatThrownBy(
            () ->
                policy.validate(window("2026-10-29T10:00:00Z", "2026-10-29T11:01:00Z"), now, true))
        .isInstanceOf(AppException.class);
  }

  @Test
  void rejectsCrossMidnight() {
    assertThatThrownBy(
            () ->
                policy.validate(window("2026-10-29T10:00:00Z", "2026-10-30T02:00:00Z"), now, true))
        .isInstanceOf(AppException.class);
  }

  @Test
  void rejectsSubMinutePrecision() {
    assertThatThrownBy(
            () ->
                policy.validate(window("2026-10-29T02:00:01Z", "2026-10-29T03:00:00Z"), now, true))
        .isInstanceOf(AppException.class);
  }

  @Test
  void rejectsPastStart() {
    assertThatThrownBy(
            () ->
                policy.validate(window("2026-10-27T02:00:00Z", "2026-10-27T03:00:00Z"), now, true))
        .isInstanceOf(AppException.class);
  }

  @Test
  void rejectsEmptyWindow() {
    assertThatThrownBy(() -> window("2026-10-29T02:00:00Z", "2026-10-29T02:00:00Z"))
        .isInstanceOf(AppException.class);
  }

  @Test
  void dailyRecurrenceSkipsSunday() {
    var windows =
        policy.expand(
            window("2026-10-31T02:00:00Z", "2026-10-31T03:00:00Z"),
            new Recurrence("daily", 1, 3),
            now);
    assertThat(windows.stream().map(w -> w.startsAt().atZone(MeetingPolicy.ZONE).getDayOfWeek()))
        .containsExactly(DayOfWeek.SATURDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
  }

  @Test
  void weeklyRecurrenceKeepsLocalTime() {
    var windows =
        policy.expand(
            window("2026-10-29T02:00:00Z", "2026-10-29T03:00:00Z"),
            new Recurrence("weekly", 2, 3),
            now);
    assertThat(windows.get(2).startsAt()).isEqualTo(Instant.parse("2026-11-26T02:00:00Z"));
  }

  @Test
  void rejectsUnboundedHorizon() {
    assertThatThrownBy(
            () ->
                policy.expand(
                    window("2026-10-29T02:00:00Z", "2026-10-29T03:00:00Z"),
                    new Recurrence("weekly", 12, 52),
                    now))
        .isInstanceOf(AppException.class);
  }

  @Test
  void rejectsUnsupportedRecurrence() {
    assertThatThrownBy(() -> new Recurrence("monthly", 1, 3)).isInstanceOf(AppException.class);
  }

  @Test
  void rejectsDuplicateStrategyRegistrations() {
    assertThatThrownBy(
            () ->
                new MeetingPolicy(
                    List.of(new DailyRecurrenceStrategy(), new DailyRecurrenceStrategy())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Duplicate recurrence strategy");
  }

  @Test
  void rejectsIncompleteStrategyRegistrations() {
    assertThatThrownBy(() -> new MeetingPolicy(List.of(new DailyRecurrenceStrategy())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Daily and weekly strategies are required");
  }
}
