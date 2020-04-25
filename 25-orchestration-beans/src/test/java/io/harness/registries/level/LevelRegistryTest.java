package io.harness.registries.level;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.registries.DuplicateRegistryException;
import io.harness.registries.RegistryType;
import io.harness.registries.UnregisteredKeyAccess;
import io.harness.rule.Owner;
import io.harness.state.io.ambiance.Level;
import io.harness.utils.levels.PhaseTestLevel;
import io.harness.utils.levels.SectionTestLevel;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LevelRegistryTest extends OrchestrationBeansTest {
  @Inject private LevelRegistry levelRegistry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    Level phaseLevel = PhaseTestLevel.builder().build();
    Level sectionLevel = SectionTestLevel.builder().build();
    levelRegistry.register(phaseLevel);
    levelRegistry.register(sectionLevel);

    Level registeredPhaseLevel = levelRegistry.obtain("PHASE");
    assertThat(registeredPhaseLevel).isNotNull();

    Level registeredSectionLevel = levelRegistry.obtain("SECTION");
    assertThat(registeredSectionLevel).isNotNull();

    assertThatThrownBy(() -> levelRegistry.register(phaseLevel)).isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> levelRegistry.obtain("STEP")).isInstanceOf(UnregisteredKeyAccess.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(levelRegistry.getType()).isEqualTo(RegistryType.LEVEL);
  }
}