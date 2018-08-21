package io.harness.distribution.constraint;

import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.distribution.constraint.Consumer.State.RUNNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.distribution.constraint.Constraint.Spec;
import io.harness.distribution.constraint.Constraint.Strategy;
import org.junit.Test;

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
        .isThrownBy(() -> constraint.registerConsumer(consumer1, -5, registry));
    assertThatExceptionOfType(InvalidPermitsException.class)
        .isThrownBy(() -> constraint.registerConsumer(consumer1, 0, registry));
    assertThatExceptionOfType(InvalidPermitsException.class)
        .isThrownBy(() -> constraint.registerConsumer(consumer1, 11, registry));
  }

  @Test
  public void testRegisterConsumerFIFO()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.FIFO).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 1, registry)).isEqualTo(RUNNING);
    assertThat(constraint.registerConsumer(consumer2, 10, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer3, 1, registry)).isEqualTo(BLOCKED);
  }

  @Test
  public void testRegisterConsumerASAP()
      throws UnableToSaveConstraintException, InvalidPermitsException, UnableToRegisterConsumerException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.ASAP).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 1, registry)).isEqualTo(RUNNING);
    assertThat(constraint.registerConsumer(consumer2, 10, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer3, 1, registry)).isEqualTo(RUNNING);
    assertThat(constraint.registerConsumer(consumer4, 9, registry)).isEqualTo(BLOCKED);
  }

  @Test
  public void testRegisterConsumerUnblocked() throws UnableToSaveConstraintException, InvalidPermitsException,
                                                     UnableToRegisterConsumerException, InvalidStateException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.ASAP).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 10, registry)).isEqualTo(RUNNING);
    assertThat(constraint.registerConsumer(consumer2, 3, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer3, 5, registry)).isEqualTo(BLOCKED);
    assertThat(constraint.registerConsumer(consumer4, 5, registry)).isEqualTo(BLOCKED);

    assertThat(constraint.consumerFinished(consumer1, registry)).isTrue();

    // Unblock with wrong currently running
    assertThat(constraint.consumerUnblocked(consumer2, 5, registry)).isFalse();

    // Unblock already finished
    assertThatExceptionOfType(InvalidStateException.class)
        .isThrownBy(() -> constraint.consumerUnblocked(consumer1, 0, registry));

    assertThat(constraint.consumerUnblocked(consumer2, 0, registry)).isTrue();

    // Unblock already running
    assertThatExceptionOfType(InvalidStateException.class)
        .isThrownBy(() -> constraint.consumerUnblocked(consumer2, 3, registry));

    assertThat(constraint.consumerUnblocked(consumer3, 3, registry)).isTrue();

    // Unblock  exceeds limit
    assertThat(constraint.consumerUnblocked(consumer4, 8, registry)).isFalse();
  }

  @Test
  public void testRegisterConsumerFinished() throws UnableToSaveConstraintException, InvalidPermitsException,
                                                    UnableToRegisterConsumerException, InvalidStateException {
    ConstraintRegistry registry = new InprocConstraintRegistry();

    Constraint constraint = Constraint.create(id, Spec.builder().strategy(Strategy.ASAP).limits(10).build(), registry);
    assertThat(constraint.registerConsumer(consumer1, 10, registry)).isEqualTo(RUNNING);
    assertThat(constraint.registerConsumer(consumer2, 3, registry)).isEqualTo(BLOCKED);

    // finish blocked
    assertThatExceptionOfType(InvalidStateException.class)
        .isThrownBy(() -> constraint.consumerFinished(consumer2, registry));

    assertThat(constraint.consumerFinished(consumer1, registry)).isTrue();

    // finish blocked again with no running
    assertThatExceptionOfType(InvalidStateException.class)
        .isThrownBy(() -> constraint.consumerFinished(consumer2, registry));
  }
}
