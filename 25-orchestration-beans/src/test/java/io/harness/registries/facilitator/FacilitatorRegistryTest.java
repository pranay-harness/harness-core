package io.harness.registries.facilitator;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorObtainment;
import io.harness.facilitate.FacilitatorResponse;
import io.harness.facilitate.FacilitatorType;
import io.harness.facilitate.io.FacilitatorParameters;
import io.harness.rule.Owner;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class FacilitatorRegistryTest extends OrchestrationBeansTest {
  @Inject private FacilitatorRegistry facilitatorRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    FacilitatorType facilitatorType = FacilitatorType.builder().type("Type1").build();
    FacilitatorParameters parameters = Type1FacilitatorParameters.builder().name("paramName").build();
    FacilitatorObtainment obtainment =
        FacilitatorObtainment.builder().type(facilitatorType).parameters(parameters).build();
    facilitatorRegistry.register(facilitatorType, new Type1FacilitatorProducer());
    Facilitator facilitator = facilitatorRegistry.obtain(obtainment);
    assertThat(facilitator).isNotNull();
    assertThat(facilitator.getType()).isEqualTo(facilitatorType);
    Type1Facilitator type1Adviser = (Type1Facilitator) facilitator;
    assertThat(type1Adviser.getParameters()).isEqualTo(parameters);
    assertThat(type1Adviser.getParameters().getName()).isEqualTo("paramName");
  }

  private static class Type1FacilitatorProducer implements FacilitatorProducer {
    @Override
    public Type1Facilitator produce(FacilitatorParameters adviserParameters) {
      return Type1Facilitator.builder().parameters((Type1FacilitatorParameters) adviserParameters).build();
    }

    @Override
    public FacilitatorType getType() {
      return FacilitatorType.builder().type("Type1").build();
    }
  }

  @Value
  @Builder
  private static class Type1FacilitatorParameters implements FacilitatorParameters {
    String name;
  }

  @Value
  @Builder
  private static class Type1Facilitator implements Facilitator {
    Type1FacilitatorParameters parameters;

    @Override
    public FacilitatorType getType() {
      return FacilitatorType.builder().type("Type1").build();
    }

    @Override
    public FacilitatorResponse facilitate(Ambiance ambiance, List<StateTransput> inputs) {
      return null;
    }
  }
}