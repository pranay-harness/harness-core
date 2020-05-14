package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.levels.PhaseLevel;
import io.harness.redesign.levels.SectionLevel;
import io.harness.redesign.levels.StepLevel;
import io.harness.registries.registrar.LevelRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class WingsLevelRegistrar implements LevelRegistrar {
  @Override
  public void register(Set<Pair<LevelType, Class<? extends Level>>> levelClasses) {
    levelClasses.add(Pair.of(PhaseLevel.LEVEL_TYPE, PhaseLevel.class));
    levelClasses.add(Pair.of(SectionLevel.LEVEL_TYPE, SectionLevel.class));
    levelClasses.add(Pair.of(StepLevel.LEVEL_TYPE, StepLevel.class));
  }
}
