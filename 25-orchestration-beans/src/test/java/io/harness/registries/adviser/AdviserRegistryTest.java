package io.harness.registries.adviser;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;
import com.google.inject.Injector;

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
  @Inject private Injector injector;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    AdviserType adviserType = AdviserType.builder().type("Type1").build();
    AdviserParameters parameters = Type1AdviserParameters.builder().name("paramName").build();
    AdviserObtainment obtainment = AdviserObtainment.builder().type(adviserType).parameters(parameters).build();
    adviserRegistry.register(adviserType, injector.getInstance(Type1Adviser.class));
    Adviser adviser = adviserRegistry.obtain(adviserType);
    assertThat(adviser).isNotNull();
    assertThat(adviser.getType()).isEqualTo(adviserType);
    Type1Adviser type1Adviser = (Type1Adviser) adviser;

    assertThatThrownBy(() -> adviserRegistry.register(adviserType, injector.getInstance(Type1Adviser.class)))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> adviserRegistry.obtain(AdviserType.builder().type(AdviserType.IGNORE).build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(adviserRegistry.getType()).isEqualTo(RegistryType.ADVISER);
  }

  @Value
  @Builder
  private static class Type1AdviserParameters implements AdviserParameters {
    String name;
  }

  private static class Type1Adviser implements Adviser {
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