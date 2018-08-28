package io.harness.distribution.constraint;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.distribution.constraint.Consumer.State.FINISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.distribution.constraint.Constraint.Spec;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.threading.Concurrent;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ConstraintTest {
  ConstraintId id = new ConstraintId("foo");

  ConsumerId consumer1 = new ConsumerId("consumer1");
  ConsumerId consumer2 = new ConsumerId("consumer2");
  ConsumerId consumer3 = new ConsumerId("consumer3");
  ConsumerId consumer4 = new ConsumerId("consumer4");

  @Test
  public void testCreateConstraint() throws UnableToSaveConstraintException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.FIFO).limits(10).build(), registry);
    assertThat(constraint).isNotNull();
  }

  @Test
  public void testLoadConstraint() throws UnableToSaveConstraintException, UnableToLoadConstraintException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.load(id, registry);
    assertThat(constraint).isNull();

    Constraint newConstraint =
        Constraint.create(id, Spec.builder().strategy(Strategy.FIFO).limits(10).build(), registry);

    constraint = Constraint.load(id, registry);

    assertThat(constraint).isEqualTo(newConstraint);
  }

  @Test
  public void testRegisterConsumer()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.FIFO).limits(10).build(), registry);

    assertThatExceptionOfType(InvalidPermitsException.class)
        .isThrownBy(() -> constraint.registerConsumer(consumer1, -5, null, registry));
    assertThatExceptionOfType(InvalidPermitsException.class)
        .isThrownBy(() -> constraint.registerConsumer(consumer1, 0, null, registry));
    assertThatExceptionOfType(InvalidPermitsException.class)
        .isThrownBy(() -> constraint.registerConsumer(consumer1, 11, null, registry));
  }

  @Test
  public void testRegisterConsumerFIFO()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.FIFO).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 1, null, registry)).isEqualTo(ACTIVE);
    assertThat(constraint.registerConsumer(consumer2, 10, null, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer3, 1, null, registry)).isEqualTo(BLOCKED);
  }

  @Test
  public void testRegisterConsumerASAP()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.ASAP).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 1, null, registry)).isEqualTo(ACTIVE);
    assertThat(constraint.registerConsumer(consumer2, 10, null, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer3, 1, null, registry)).isEqualTo(ACTIVE);
    assertThat(constraint.registerConsumer(consumer4, 9, null, registry)).isEqualTo(BLOCKED);
  }

  @Test
  public void testRegisterConsumerUnblocked()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.ASAP).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 10, null, registry)).isEqualTo(ACTIVE);
    assertThat(constraint.registerConsumer(consumer2, 3, null, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer3, 5, null, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer4, 5, null, registry)).isEqualTo(BLOCKED);

    assertThat(constraint.consumerFinished(consumer1, registry)).isTrue();

    // Unblock already finished
    assertThat(constraint.consumerUnblocked(consumer1, null, registry)).isFalse();

    assertThat(constraint.consumerUnblocked(consumer2, null, registry)).isTrue();

    // Unblock already running
    assertThat(constraint.consumerUnblocked(consumer2, null, registry)).isFalse();

    assertThat(constraint.consumerUnblocked(consumer3, null, registry)).isTrue();
  }

  @Test
  public void testRegisterConsumerFinished()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.ASAP).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 10, null, registry)).isEqualTo(ACTIVE);
    assertThat(constraint.registerConsumer(consumer2, 3, null, registry)).isEqualTo(BLOCKED);

    // finish blocked
    assertThat(constraint.consumerFinished(consumer2, registry)).isFalse();

    assertThat(constraint.consumerFinished(consumer1, registry)).isTrue();

    // finish blocked again with no running
    assertThat(constraint.consumerFinished(consumer2, registry)).isFalse();
  }

  @Test
  public void testRunnableConsumersASAP()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.ASAP).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 10, null, registry)).isEqualTo(ACTIVE);
    assertThat(constraint.registerConsumer(consumer2, 3, null, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer3, 8, null, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer4, 3, null, registry)).isEqualTo(BLOCKED);

    assertThat(constraint.runnableConsumers(registry).getConsumerIds()).isEmpty();

    assertThat(constraint.consumerFinished(consumer1, registry)).isTrue();

    assertThat(constraint.runnableConsumers(registry).getConsumerIds()).contains(consumer2, consumer4);
  }

  @Test
  public void testRunnableConsumersFIFO()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.FIFO).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 10, null, registry)).isEqualTo(ACTIVE);
    assertThat(constraint.registerConsumer(consumer2, 3, null, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer3, 8, null, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer4, 3, null, registry)).isEqualTo(BLOCKED);

    assertThat(constraint.runnableConsumers(registry).getConsumerIds()).isEmpty();

    assertThat(constraint.consumerFinished(consumer1, registry)).isTrue();

    assertThat(constraint.runnableConsumers(registry).getConsumerIds()).contains(consumer2);
  }

  @Test
  public void testSimulation()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.FIFO).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 10, null, registry)).isEqualTo(ACTIVE);

    final Random random = new Random();
    for (int i = 0; i < 100; ++i) {
      ConsumerId consumerId = new ConsumerId("c" + i);
      assertThat(constraint.registerConsumer(consumerId, random.nextInt(10) + 1, null, registry)).isEqualTo(BLOCKED);
    }

    Concurrent.test(11, i -> {
      while (true) {
        if (i == 0) {
          final List<Consumer> consumers = registry.loadConsumers(constraint.getId());
          if (consumers.stream().noneMatch(consumer -> consumer.getState() != FINISHED)) {
            break;
          }
          final List<Consumer> list =
              consumers.stream().filter(consumer -> consumer.getState() == ACTIVE).collect(Collectors.toList());
          if (isEmpty(list)) {
            continue;
          }
          final Consumer consumer = list.get(random.nextInt(list.size()));
          registry.consumerFinished(constraint.getId(), consumer.getId(), null);
        } else {
          final RunnableConsumers runnableConsumers = constraint.runnableConsumers(registry);
          if (runnableConsumers.getUsedPermits() == 0 && isEmpty(runnableConsumers.getConsumerIds())) {
            break;
          }

          for (ConsumerId consumerId : runnableConsumers.getConsumerIds()) {
            if (!constraint.consumerUnblocked(consumerId, null, registry)) {
              break;
            }
          }
        }
      }
    });

    final List<Consumer> consumers = registry.loadConsumers(constraint.getId());
    assertThat(consumers.stream().anyMatch(consumer -> consumer.getState() != FINISHED)).isFalse();
  }
}
