package io.harness.distribution.constraint;

import io.harness.distribution.constraint.Constraint.Spec;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

public interface ConstraintRegistry {
  void save(ConstraintId id, Spec spec) throws UnableToSaveConstraintException;
  Constraint load(ConstraintId id) throws UnableToLoadConstraintException;
  List<Consumer> loadConsumers(@NotNull ConstraintId id, @NotNull ConstraintUnit unit);

  // When a new consumer is registered it goes into either blocked or running state.
  boolean registerConsumer(ConstraintId id, ConstraintUnit unit, Consumer consumer, int currentlyRunning)
      throws UnableToRegisterConsumerException;

  boolean adjustRegisterConsumerContext(ConstraintId id, Map<String, Object> context);

  boolean consumerUnblocked(ConstraintId id, ConstraintUnit unit, ConsumerId consumerId, Map<String, Object> context);
  boolean consumerFinished(ConstraintId id, ConstraintUnit unit, ConsumerId consumerId, Map<String, Object> context);

  boolean overlappingScope(Consumer consumer, Consumer blockedConsumer);

  boolean finishAndUnblockConsumers(
      ConstraintId id, ConstraintUnit unit, ConsumerId consumerId, Map<String, Object> context);
}
