package io.harness.registries.adviser;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.registries.AdviserRegistry;
import io.harness.pms.sdk.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AdviserRegistryTest extends OrchestrationBeansTestBase {
  @Inject private Injector injector;
  @Inject private AdviserRegistry adviserRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    AdviserType adviserType = AdviserType.newBuilder().setType("Type1").build();
    adviserRegistry.register(adviserType, injector.getInstance(Type1Adviser.class));
    Adviser adviser = adviserRegistry.obtain(adviserType);
    assertThat(adviser).isNotNull();

    assertThatThrownBy(() -> adviserRegistry.register(adviserType, injector.getInstance(Type1Adviser.class)))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> adviserRegistry.obtain(AdviserType.newBuilder().setType("Type2").build()))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(adviserRegistry.getType()).isEqualTo(RegistryType.ADVISER.name());
  }

  private static class Type1Adviser implements Adviser {
    @Override
    public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
      return null;
    }

    @Override
    public boolean canAdvise(AdvisingEvent advisingEvent) {
      return false;
    }
  }
}
