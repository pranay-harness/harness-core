package io.harness.distribution.constraint;

import io.harness.distribution.constraint.Constraint.Spec;

import java.util.List;

public interface ConstraintRegistry {
  void save(ConstraintId id, Spec spec) throws UnableToSaveConstraintException;
  Constraint load(ConstraintId id) throws UnableToLoadConstraintException;
  List<Consumer> loadConsumers(ConstraintId id);

  // When a new consumer is registered it goes into either blocked or running state.
  boolean registerConsumer(ConstraintId id, Consumer consumer, int currentlyRunning)
      throws UnableToRegisterConsumerException;

  interface ExtraCheck {
    boolean check(List<Consumer> constraintConsumers, Consumer consumer);
  }

  boolean consumerUnblocked(ConstraintId id, ConsumerId consumerId, ExtraCheck extraCheck) throws InvalidStateException;
  boolean consumerFinished(ConstraintId id, ConsumerId consumerId) throws InvalidStateException;
}
