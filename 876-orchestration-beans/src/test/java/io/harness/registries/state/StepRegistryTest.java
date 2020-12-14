package io.harness.registries.state;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.EmptyStepParameters;
import io.harness.pms.sdk.registries.RegistryType;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepRegistryTest extends OrchestrationBeansTestBase {
  @Inject private StepRegistry stepRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    StepType stepType = StepType.newBuilder().setType("DUMMY_TEST").build();
    stepRegistry.register(stepType, new DummyStep());
    Step step = stepRegistry.obtain(stepType);
    assertThat(step).isNotNull();

    assertThatThrownBy(() -> stepRegistry.register(stepType, new DummyStep()))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> stepRegistry.obtain(StepType.newBuilder().setType("RANDOM").build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(stepRegistry.getType()).isEqualTo(RegistryType.STEP.name());
  }

  @Value
  @Builder
  private static class DummyStep implements Step<EmptyStepParameters> {
    @Override
    public Class<EmptyStepParameters> getStepParametersClass() {
      return EmptyStepParameters.class;
    }
  }
}
