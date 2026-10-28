package com.dki.meeting.booking.domain;

import static org.assertj.core.api.Assertions.*;

import com.dki.meeting.foundation.AppException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class BookingFactoryTest {
  private final Instant now = Instant.parse("2026-10-28T00:00:00Z");
  private final UUID organizer = UUID.randomUUID();
  private final AtomicLong ids = new AtomicLong();

  private BookingDraft draft() {
    return new BookingDraft(
        UUID.randomUUID(),
        "Planning",
        null,
        null,
        List.of(new TimeWindow(now.plusSeconds(3600), now.plusSeconds(7200))),
        List.of(UUID.randomUUID()));
  }

  private Booking create() {
    return BookingFactory.create(
        UUID.randomUUID(), organizer, draft(), now, () -> new UUID(0, ids.incrementAndGet()));
  }

  @Test
  void factoryCreatesPendingChildrenWithExplicitTimeAndIds() {
    var booking = create();
    assertThat(booking.status()).isEqualTo("pending");
    assertThat(booking.organizerId()).isEqualTo(organizer);
    assertThat(booking.createdAt()).isEqualTo(now);
    assertThat(booking.occurrences().get(0).id()).isEqualTo(new UUID(0, 1));
    assertThat(booking.attendees().get(0).response()).isEqualTo("pending");
    assertThatThrownBy(() -> booking.occurrences().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void reschedulingPreservesIdentityAndCreationTimeButResetsTheDecision() {
    var original = create();
    var revised =
        BookingFactory.reschedule(original, draft(), now.plusSeconds(30), UUID::randomUUID);
    assertThat(revised.id()).isEqualTo(original.id());
    assertThat(revised.organizerId()).isEqualTo(original.organizerId());
    assertThat(revised.createdAt()).isEqualTo(original.createdAt());
    assertThat(revised.updatedAt()).isEqualTo(now.plusSeconds(30));
    assertThat(revised.version()).isEqualTo(1);
    assertThat(revised.status()).isEqualTo("pending");
    assertThat(revised.decidedBy()).isNull();
  }

  @Test
  void aggregateRejectsStartedEditsAndStaleVersions() {
    var booking = create();
    assertThatThrownBy(() -> booking.requireEditable(now.plusSeconds(3600)))
        .isInstanceOf(AppException.class);
    assertThatThrownBy(() -> booking.requireVersion(1)).isInstanceOf(AppException.class);
    assertThatCode(() -> booking.requireVersion(0)).doesNotThrowAnyException();
  }

  @Test
  void closedStatesRejectApprovalCancellationAndResponses() {
    var original = create();
    for (String state : List.of("rejected", "cancelled")) {
      var closed =
          new Booking(
              original.id(),
              original.roomId(),
              original.organizerId(),
              original.title(),
              null,
              state,
              null,
              1,
              now,
              now,
              organizer,
              "Closed",
              original.occurrences(),
              original.attendees());
      assertThatThrownBy(closed::requirePending).isInstanceOf(AppException.class);
      assertThatThrownBy(() -> closed.requireCancelable(now)).isInstanceOf(AppException.class);
      assertThatThrownBy(() -> closed.requireAcceptingResponses(now))
          .isInstanceOf(AppException.class);
    }
  }

  @Test
  void cancellationNeedsAFutureStartButResponsesCanContinueDuringTheMeeting() {
    var booking = create();
    assertThatCode(() -> booking.requireCancelable(now)).doesNotThrowAnyException();
    assertThatThrownBy(() -> booking.requireCancelable(now.plusSeconds(3600)))
        .isInstanceOf(AppException.class);
    assertThatCode(() -> booking.requireAcceptingResponses(now.plusSeconds(3600)))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> booking.requireAcceptingResponses(now.plusSeconds(7200)))
        .isInstanceOf(AppException.class);
  }

  @Test
  void draftRejectsEmptySeriesAndDuplicateInvitees() {
    assertThatThrownBy(
            () -> new BookingDraft(UUID.randomUUID(), "Planning", null, null, List.of(), List.of()))
        .isInstanceOf(AppException.class);
    UUID invited = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                new BookingDraft(
                    UUID.randomUUID(),
                    "Planning",
                    null,
                    null,
                    draft().windows(),
                    List.of(invited, invited)))
        .isInstanceOf(AppException.class);
  }
}
