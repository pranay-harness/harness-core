package io.harness.distribution.constraint;

import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.distribution.constraint.Consumer.State.FINISHED;
import static io.harness.distribution.constraint.Consumer.State.RUNNING;
import static java.util.Collections.synchronizedMap;

import io.harness.distribution.constraint.Constraint.Spec;
import io.harness.distribution.constraint.Consumer.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class InprocConstraintRegistry implements ConstraintRegistry {
  private Map<ConstraintId, Spec> map = synchronizedMap(new HashMap<>());
  private Map<ConstraintId, List<Consumer>> consumers = synchronizedMap(new HashMap<>());

  @Override
  public void save(ConstraintId id, Spec spec) throws UnableToSaveConstraintException {
    if (map.putIfAbsent(id, spec) != null) {
      throw new UnableToSaveConstraintException("The constraint with this id already exists");
    }
  }

  @Override
  public Constraint load(ConstraintId id) throws UnableToLoadConstraintException {
    final Spec spec = map.get(id);
    if (spec == null) {
      return null;
    }
    return Constraint.builder().id(id).spec(spec).build();
  }

  @Override
  public List<Consumer> loadConsumers(ConstraintId id) {
    return this.consumers.computeIfAbsent(id, key -> new ArrayList<>());
  }

  @Override
  public boolean registerConsumer(ConstraintId id, Consumer consumer, int currentlyRunning)
      throws UnableToRegisterConsumerException {
    synchronized (consumers) {
      List<Consumer> constraintConsumers = consumers.computeIfAbsent(id, key -> new ArrayList<>());
      if (Constraint.getUsedPermits(constraintConsumers) != currentlyRunning) {
        return false;
      }

      constraintConsumers.add(consumer);
    }
    return true;
  }

  @Override
  public boolean consumerFinished(ConstraintId id, ConsumerId consumerId) {
    return consumerStateChange(id, consumerId, FINISHED, RUNNING, null);
  }

  @Override
  public boolean consumerUnblocked(ConstraintId id, ConsumerId consumerId, ExtraCheck extraCheck) {
    return consumerStateChange(id, consumerId, RUNNING, BLOCKED, extraCheck);
  }

  private boolean consumerStateChange(
      ConstraintId id, ConsumerId consumerId, State newState, State expected, ExtraCheck extraCheck) {
    synchronized (consumers) {
      final List<Consumer> constraintConsumers = this.consumers.get(id);

      final ListIterator<Consumer> iterator = constraintConsumers.listIterator();
      while (iterator.hasNext()) {
        Consumer consumer = iterator.next();
        if (consumer.getId().equals(consumerId)) {
          if (consumer.getState() != expected) {
            return false;
          }
          if (extraCheck != null && !extraCheck.check(constraintConsumers, consumer)) {
            return false;
          }
          iterator.set(Consumer.builder().state(newState).id(consumer.getId()).permits(consumer.getPermits()).build());
          return true;
        }
      }
    }
    return false;
  }
}
