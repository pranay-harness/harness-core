package io.harness.registries.level;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.OrchestrationBeansTest;
import io.harness.ambiance.Level;
import io.harness.category.element.UnitTests;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.rule.Owner;
import io.harness.utils.levels.PhaseTestLevel;
import io.harness.utils.levels.SectionTestLevel;
import io.harness.utils.levels.StepTestLevel;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LevelRegistryTest extends OrchestrationBeansTest {
  @Inject private LevelRegistry levelRegistry;
  @Inject private Injector injector;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    levelRegistry.register(PhaseTestLevel.LEVEL_TYPE, injector.getInstance(PhaseTestLevel.class));
    levelRegistry.register(SectionTestLevel.LEVEL_TYPE, injector.getInstance(SectionTestLevel.class));

    Level registeredPhaseLevel = levelRegistry.obtain(PhaseTestLevel.LEVEL_TYPE);
    assertThat(registeredPhaseLevel).isNotNull();

    Level registeredSectionLevel = levelRegistry.obtain(SectionTestLevel.LEVEL_TYPE);
    assertThat(registeredSectionLevel).isNotNull();

    assertThatThrownBy(
        () -> levelRegistry.register(PhaseTestLevel.LEVEL_TYPE, injector.getInstance(PhaseTestLevel.class)))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> levelRegistry.obtain(StepTestLevel.LEVEL_TYPE))
        .isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(levelRegistry.getType()).isEqualTo(RegistryType.LEVEL);
  }
}