package com.dki.meeting.infra.transaction;

import com.dki.meeting.foundation.*;
import java.util.function.Predicate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalUseCase<I, O> implements UseCase<I, O> {
  private final UseCase<I, O> delegate;
  private final TransactionTemplate template;
  private final Predicate<RuntimeException> commitOnFailure;

  public TransactionalUseCase(
      UseCase<I, O> delegate,
      PlatformTransactionManager manager,
      boolean readOnly,
      Predicate<RuntimeException> commitOnFailure) {
    this.delegate = delegate;
    this.template = new TransactionTemplate(manager);
    template.setReadOnly(readOnly);
    if (readOnly) template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    this.commitOnFailure = commitOnFailure;
  }

  @Override
  public O execute(OperationContext context, I input) {
    Attempt<O> result =
        template.execute(
            status -> {
              try {
                return new Attempt<>(delegate.execute(context, input), null);
              } catch (RuntimeException error) {
                if (commitOnFailure.test(error)) return new Attempt<O>(null, error);
                throw error;
              }
            });
    if (result == null) throw new IllegalStateException("Transaction returned no result");
    if (result.error() != null) throw result.error();
    return result.value();
  }

  private record Attempt<T>(T value, RuntimeException error) {}
}
