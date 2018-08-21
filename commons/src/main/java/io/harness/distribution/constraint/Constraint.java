package io.harness.distribution.constraint;

import static io.harness.distribution.constraint.Constraint.Strategy.ASAP;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.distribution.constraint.Consumer.State.RUNNING;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import io.harness.distribution.constraint.Consumer.State;
import io.harness.threading.Morpheus;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.List;
import java.util.Random;

/*
 * Distributed constrain is designed to limit the access to arbitrary resource. It allows for configurable number of
 * permits. Every request requests particular number of permits. The constrain primitive will allow it if the expected
 * permits are available and will block it otherwise.
 *
 * Before we get in details lets put a small glossary:
 * * resource - an arbitrary system need that has some limits that can be represented in number
 * * limits - a number that represent the resource limitation
 * * consumer - a system that consumes at whole or partially the resource
 * * permits - a number that consume needs during it execution
 * * constrain - system that allows for multiple consumers to utilize a resource without permits of the active consumer
 * *             to exceed the limits of the resource
 * * constrain registry - a storage where the constrain can be persisted in a distributed way
 * * strategy - the way constrain allows for obtaining new permits
 * * as soon as possible strategy - In this strategy the constrain will allow for the next in line consumer that have
 *                                  the needed permits available to go.
 * * fifo or fair strategy - it will allow the consumers that are fist in to continue first
 *
 */

@Value
@Builder
public class Constraint {
  private static final int OPTIMISTIC_RETRIES = 10;
  private static final int DELAY_FOR_OPTIMISTIC_RETRIES = 10;

  private ConstraintId id;

  public enum Strategy {
    ASAP,
    FIFO,
  }

  @Value
  @Builder
  public static class Spec {
    @Builder.Default private Strategy strategy = ASAP;
    int limits;
  }

  private Spec spec;

  public static Constraint create(ConstraintId id, Spec spec, ConstraintRegistry registry)
      throws UnableToSaveConstraintException {
    registry.save(id, spec);

    return builder().id(id).spec(spec).build();
  }

  public static Constraint load(ConstraintId id, ConstraintRegistry registry) throws UnableToLoadConstraintException {
    return registry.load(id);
  }

  public static int getUsedPermits(List<Consumer> consumers) {
    return consumers.stream().filter(item -> item.getState().equals(RUNNING)).mapToInt(item -> item.getPermits()).sum();
  }

  private boolean enoughPermits(int permits, int usedPermits) {
    return getSpec().getLimits() - usedPermits >= permits;
  }

  private boolean nonBlocked(List<Consumer> consumers) {
    return consumers.stream().noneMatch(item -> item.getState().equals(BLOCKED));
  }

  public State registerConsumer(ConsumerId consumerId, int permits, ConstraintRegistry registry)
      throws InvalidPermitsException, UnableToRegisterConsumerException {
    if (permits <= 0) {
      throw new InvalidPermitsException("The permits should be positive number.");
    }
    if (permits > spec.getLimits()) {
      throw new InvalidPermitsException(format(
          "An amount of %d permits cannot be requested from constraint with limit %d.", permits, spec.getLimits()));
    }

    final Random random = new Random();
    for (int i = 0; i < OPTIMISTIC_RETRIES; ++i) {
      List<Consumer> consumers = registry.loadConsumers(id);
      final int usedPermits = getUsedPermits(consumers);

      State state = null;

      final Strategy strategy = getSpec().getStrategy();
      switch (strategy) {
        case FIFO:
          state = nonBlocked(consumers) && enoughPermits(permits, usedPermits) ? RUNNING : BLOCKED;
          break;
        case ASAP:
          state = enoughPermits(permits, usedPermits) ? RUNNING : BLOCKED;
          break;
        default:
          unhandled(strategy);
      }

      if (registry.registerConsumer(
              id, Consumer.builder().id(consumerId).permits(permits).state(state).build(), usedPermits)) {
        return state;
      }

      Morpheus.quietSleep(Duration.ofMillis(random.nextInt(DELAY_FOR_OPTIMISTIC_RETRIES)));
    }

    throw new RuntimeException("Unable to update the constraint after 10 attempts.");
  }

  boolean consumerUnblocked(ConsumerId consumerId, int currentlyRunning, ConstraintRegistry registry)
      throws InvalidStateException {
    return registry.consumerUnblocked(id, consumerId,
        (constraintConsumers, consumer)
            -> enoughPermits(consumer.getPermits(), currentlyRunning)
            && Constraint.getUsedPermits(constraintConsumers) == currentlyRunning);
  }

  boolean consumerFinished(ConsumerId consumerId, ConstraintRegistry registry) throws InvalidStateException {
    return registry.consumerFinished(id, consumerId);
  }
}
