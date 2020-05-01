package io.harness.registrars;

import io.harness.ambiance.Level;
import io.harness.redesign.levels.PhaseLevel;
import io.harness.redesign.levels.SectionLevel;
import io.harness.redesign.levels.StepLevel;
import io.harness.registries.registrar.LevelRegistrar;

import java.util.Set;

public class WingsLevelRegistrar implements LevelRegistrar {
  @Override
  public void register(Set<Class<? extends Level>> levelClasses) {
    levelClasses.add(PhaseLevel.class);
    levelClasses.add(SectionLevel.class);
    levelClasses.add(StepLevel.class);
  }
}
