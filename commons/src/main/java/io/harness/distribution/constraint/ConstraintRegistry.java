package io.harness.distribution.constraint;

import io.harness.distribution.constraint.Constraint.Spec;

import java.util.List;
import java.util.Map;

public interface ConstraintRegistry {
  void save(ConstraintId id, Spec spec) throws UnableToSaveConstraintException;
  Constraint load(ConstraintId id) throws UnableToLoadConstraintException;
  List<Consumer> loadConsumers(ConstraintId id);

  // When a new consumer is registered it goes into either blocked or running state.
  boolean registerConsumer(ConstraintId id, Consumer consumer, int currentlyRunning, Map<String, Object> context)
      throws UnableToRegisterConsumerException;

  boolean adjustRegisterConsumerContext(ConstraintId id, Map<String, Object> context);

  boolean consumerUnblocked(ConstraintId id, ConsumerId consumerId, Map<String, Object> context);
  boolean consumerFinished(ConstraintId id, ConsumerId consumerId, Map<String, Object> context);
}
