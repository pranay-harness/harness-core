package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.levels.PhaseLevel;
import io.harness.redesign.levels.SectionLevel;
import io.harness.redesign.levels.StepLevel;
import io.harness.registries.registrar.LevelRegistrar;

import java.util.Set;

@OwnedBy(CDC)
public class WingsLevelRegistrar implements LevelRegistrar {
  @Override
  public void register(Set<Class<? extends Level>> levelClasses) {
    levelClasses.add(PhaseLevel.class);
    levelClasses.add(SectionLevel.class);
    levelClasses.add(StepLevel.class);
  }
}
