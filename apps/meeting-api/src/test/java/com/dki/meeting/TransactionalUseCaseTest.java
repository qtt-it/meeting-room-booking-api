package com.dki.meeting;

import static org.assertj.core.api.Assertions.*;

import com.dki.meeting.foundation.*;
import com.dki.meeting.identity.application.RefreshReuseException;
import com.dki.meeting.infra.transaction.TransactionalUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class TransactionalUseCaseTest {
  private static class RecordingManager extends AbstractPlatformTransactionManager {
    int commits, rollbacks;
    TransactionDefinition definition;

    protected Object doGetTransaction() {
      return new Object();
    }

    protected void doBegin(Object transaction, TransactionDefinition definition) {
      this.definition = definition;
    }

    protected void doCommit(DefaultTransactionStatus status) {
      commits++;
    }

    protected void doRollback(DefaultTransactionStatus status) {
      rollbacks++;
    }
  }

  @Test
  void successfulCommandCommitsItsResult() {
    var manager = new RecordingManager();
    var useCase =
        new TransactionalUseCase<String, String>(
            (ctx, input) -> input.toUpperCase(), manager, false, error -> false);
    assertThat(useCase.execute(OperationContext.system(), "ok")).isEqualTo("OK");
    assertThat(manager.commits).isEqualTo(1);
    assertThat(manager.rollbacks).isZero();
    assertThat(manager.definition.isReadOnly()).isFalse();
  }

  @Test
  void ordinaryFailureRollsBackAndRetainsTheOriginalCause() {
    var manager = new RecordingManager();
    var failure = AppException.invalid("Rejected");
    var useCase =
        new TransactionalUseCase<Void, Void>(
            (ctx, input) -> {
              throw failure;
            },
            manager,
            false,
            error -> false);
    assertThatThrownBy(() -> useCase.execute(OperationContext.system(), null)).isSameAs(failure);
    assertThat(manager.commits).isZero();
    assertThat(manager.rollbacks).isEqualTo(1);
  }

  @Test
  void refreshReuseCommitsRevocationThenReturnsTheSameFailure() {
    var manager = new RecordingManager();
    var failure = new RefreshReuseException();
    var useCase =
        new TransactionalUseCase<Void, Void>(
            (ctx, input) -> {
              throw failure;
            },
            manager,
            false,
            error -> error instanceof RefreshReuseException);
    assertThatThrownBy(() -> useCase.execute(OperationContext.system(), null)).isSameAs(failure);
    assertThat(manager.commits).isEqualTo(1);
    assertThat(manager.rollbacks).isZero();
  }

  @Test
  void queryUsesAReadOnlyRepeatableReadSnapshot() {
    var manager = new RecordingManager();
    var useCase =
        new TransactionalUseCase<Void, String>(
            (ctx, input) -> "read", manager, true, error -> false);
    assertThat(useCase.execute(OperationContext.system(), null)).isEqualTo("read");
    assertThat(manager.definition.isReadOnly()).isTrue();
    assertThat(manager.definition.getIsolationLevel())
        .isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }
}
