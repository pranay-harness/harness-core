package io.harness.registries.facilitator;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FacilitatorRegistryTest extends OrchestrationBeansTestBase {
  @Inject private FacilitatorRegistry facilitatorRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    FacilitatorType facilitatorType = FacilitatorType.newBuilder().setType("Type1").build();
    facilitatorRegistry.register(facilitatorType, new Type1Facilitator());
    Facilitator facilitator = facilitatorRegistry.obtain(facilitatorType);
    assertThat(facilitator).isNotNull();

    assertThatThrownBy(() -> facilitatorRegistry.register(facilitatorType, new Type1Facilitator()))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> facilitatorRegistry.obtain(FacilitatorType.newBuilder().setType("SKIP").build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(facilitatorRegistry.getType()).isEqualTo(RegistryType.FACILITATOR.name());
  }

  @Value
  @Builder
  private static class Type1Facilitator implements Facilitator {
    @Override
    public FacilitatorResponse facilitate(
        Ambiance ambiance, StepParameters stepParameters, byte[] parameters, StepInputPackage inputPackage) {
      return null;
    }
  }
}
