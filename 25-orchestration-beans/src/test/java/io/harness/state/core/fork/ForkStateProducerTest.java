package io.harness.state.core.fork;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ForkStateProducerTest extends OrchestrationBeansTest {
  @Inject private ForkStateProducer forkStateProducer;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void produce() {
    assertThat(forkStateProducer.produce()).isNotNull();
    assertThat(forkStateProducer.produce()).isInstanceOf(ForkState.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void getType() {
    assertThat(forkStateProducer.getType()).isNotNull();
    assertThat(forkStateProducer.getType().getType()).isEqualTo("FORK");
  }
}