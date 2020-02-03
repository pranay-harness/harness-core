package io.harness.distribution.constraint;

import static io.harness.distribution.constraint.Constraint.Strategy.ASAP;
import static io.harness.distribution.constraint.Constraint.Strategy.FIFO;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import io.harness.distribution.constraint.Consumer.State;
import io.harness.threading.Morpheus;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

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
  private static final int DELAY_FOR_OPTIMISTIC_RETRIES = 10;

  private static final Random random = new Random();

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
    return consumers.stream().filter(item -> item.getState() == ACTIVE).mapToInt(Consumer::getPermits).sum();
  }

  private boolean enoughPermits(int permits, int usedPermits) {
    return getSpec().getLimits() - usedPermits >= permits;
  }

  private boolean nonBlocked(List<Consumer> consumers) {
    return consumers.stream().noneMatch(item -> item.getState() == BLOCKED);
  }

  private void checkForBadBlock(Consumer consumer, List<Consumer> consumers, ConstraintRegistry registry)
      throws PermanentlyBlockedConsumerException {
    int sameScopePermits = consumer.getPermits();
    for (Consumer blockedConsumer : consumers) {
      if (registry.overlappingScope(consumer, blockedConsumer)) {
        sameScopePermits += blockedConsumer.getPermits();
      }
    }

    if (sameScopePermits > getSpec().getLimits()) {
      throw new PermanentlyBlockedConsumerException(
          format("Consumer %s will be permanently blocked from other consumers in the same scope",
              consumer.getId().getValue()));
    }
  }

  public State registerConsumer(
      ConstraintUnit unit, ConsumerId consumerId, int permits, Map<String, Object> context, ConstraintRegistry registry)
      throws InvalidPermitsException, UnableToRegisterConsumerException, PermanentlyBlockedConsumerException {
    if (permits <= 0) {
      throw new InvalidPermitsException("The permits should be positive number.");
    }
    if (permits > spec.getLimits()) {
      throw new InvalidPermitsException(format(
          "An amount of %d permits cannot be requested from constraint with limit %d.", permits, spec.getLimits()));
    }

    do {
      List<Consumer> consumers = registry.loadConsumers(id, unit);
      final int usedPermits = getUsedPermits(consumers);

      State state = null;

      final Strategy strategy = getSpec().getStrategy();
      switch (strategy) {
        case FIFO:
          state = nonBlocked(consumers) && enoughPermits(permits, usedPermits) ? ACTIVE : BLOCKED;
          break;
        case ASAP:
          state = enoughPermits(permits, usedPermits) ? ACTIVE : BLOCKED;
          break;
        default:
          unhandled(strategy);
      }

      final Consumer consumer =
          Consumer.builder().id(consumerId).permits(permits).state(state).context(context).build();

      if (BLOCKED == state) {
        checkForBadBlock(consumer, consumers, registry);
      }

      if (registry.registerConsumer(id, unit, consumer, usedPermits)) {
        return state;
      }

      Morpheus.quietSleep(Duration.ofMillis(random.nextInt(DELAY_FOR_OPTIMISTIC_RETRIES)));
    } while (registry.adjustRegisterConsumerContext(id, context));

    throw new UnableToRegisterConsumerException("Unable to register the constraint.");
  }

  public boolean consumerUnblocked(
      ConstraintUnit unit, ConsumerId consumerId, Map<String, Object> context, ConstraintRegistry registry) {
    return registry.consumerUnblocked(id, unit, consumerId, context);
  }

  boolean consumerFinished(ConstraintUnit unit, ConsumerId consumerId, ConstraintRegistry registry) {
    return registry.consumerFinished(id, unit, consumerId, null);
  }

  public RunnableConsumers runnableConsumers(ConstraintUnit unit, ConstraintRegistry registry) {
    final List<Consumer> consumers = registry.loadConsumers(id, unit);
    return RunnableConsumers.builder()
        .usedPermits(getUsedPermits(consumers))
        .consumerIds(filterConsumersForPermitsAndStrategy(consumers))
        .build();
  }

  public List<ConsumerId> getUnblockableConsumer(
      ConstraintUnit unit, ConstraintRegistry registry, List<ConsumerId> filteredConsumers) {
    final List<Consumer> consumers = registry.loadConsumers(id, unit)
                                         .stream()
                                         .filter(consumer -> !filteredConsumers.contains(consumer.getId()))
                                         .collect(Collectors.toList());

    return filterConsumersForPermitsAndStrategy(consumers);
  }

  private List<ConsumerId> filterConsumersForPermitsAndStrategy(List<Consumer> consumers) {
    List<ConsumerId> consumerIds = new ArrayList<>();
    int usedPermits = getUsedPermits(consumers);
    for (Consumer consumer : consumers) {
      if (consumer.getState() != BLOCKED) {
        continue;
      }

      if (!enoughPermits(consumer.getPermits(), usedPermits)) {
        final Strategy strategy = getSpec().getStrategy();
        if (strategy == FIFO) {
          break;
        } else if (strategy == ASAP) {
          continue;
        } else {
          unhandled(strategy);
        }
      }

      consumerIds.add(consumer.getId());
      usedPermits += consumer.getPermits();
    }
    return consumerIds;
  }
}
