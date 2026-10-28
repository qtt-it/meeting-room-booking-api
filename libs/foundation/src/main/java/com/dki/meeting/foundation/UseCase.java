package com.dki.meeting.foundation;

@FunctionalInterface
public interface UseCase<I, O> {
  O execute(OperationContext context, I input);
}
