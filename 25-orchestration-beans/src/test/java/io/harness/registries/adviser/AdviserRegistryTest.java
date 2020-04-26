package io.harness.registries.adviser;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserParameters;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.category.element.UnitTests;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.rule.Owner;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AdviserRegistryTest extends OrchestrationBeansTest {
  @Inject private AdviserRegistry adviserRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    AdviserType adviserType = AdviserType.builder().type("Type1").build();
    AdviserParameters parameters = Type1AdviserParameters.builder().name("paramName").build();
    AdviserObtainment obtainment = AdviserObtainment.builder().type(adviserType).parameters(parameters).build();
    adviserRegistry.register(adviserType, new Type1AdviserProducer());
    Adviser adviser = adviserRegistry.obtain(obtainment);
    assertThat(adviser).isNotNull();
    assertThat(adviser.getType()).isEqualTo(adviserType);
    Type1Adviser type1Adviser = (Type1Adviser) adviser;
    assertThat(type1Adviser.getParameters()).isEqualTo(parameters);
    assertThat(type1Adviser.getParameters().getName()).isEqualTo("paramName");

    assertThatThrownBy(() -> adviserRegistry.register(adviserType, new Type1AdviserProducer()))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(
        ()
            -> adviserRegistry.obtain(
                AdviserObtainment.builder().type(AdviserType.builder().type(AdviserType.IGNORE).build()).build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(adviserRegistry.getType()).isEqualTo(RegistryType.ADVISER);
  }

  private static class Type1AdviserProducer implements AdviserProducer {
    @Override
    public AdviserType getType() {
      return AdviserType.builder().type("Type1").build();
    }

    @Override
    public Adviser produce(AdviserParameters adviserParameters) {
      return Type1Adviser.builder().parameters((Type1AdviserParameters) adviserParameters).build();
    }
  }

  @Value
  @Builder
  private static class Type1AdviserParameters implements AdviserParameters {
    String name;
  }

  @Value
  @Builder
  private static class Type1Adviser implements Adviser {
    Type1AdviserParameters parameters;

    @Override
    public AdviserType getType() {
      return AdviserType.builder().type("Type1").build();
    }

    @Override
    public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
      return null;
    }
  }
}