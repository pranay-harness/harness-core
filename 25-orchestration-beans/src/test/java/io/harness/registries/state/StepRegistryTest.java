package io.harness.registries.state;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.rule.Owner;
import io.harness.state.StateType;
import io.harness.state.Step;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepRegistryTest extends OrchestrationBeansTest {
  @Inject private StepRegistry stepRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    StateType stateType = StateType.builder().type("DUMMY_TEST").build();
    stepRegistry.register(stateType, DummyStep.class);
    Step step = stepRegistry.obtain(stateType);
    assertThat(step).isNotNull();

    assertThatThrownBy(() -> stepRegistry.register(stateType, DummyStep.class))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> stepRegistry.obtain(StateType.builder().type("RANDOM").build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(stepRegistry.getType()).isEqualTo(RegistryType.STEP);
  }

  @Value
  @Builder
  private static class DummyStep implements Step {
    @Override
    public StateType getType() {
      return StateType.builder().type("DUMMY_TEST").build();
    }
  }
}