package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import io.harness.annotations.dev.OwnedBy;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface LevelRegistrar extends EngineRegistrar<LevelType, Level> {
  void register(Set<Pair<LevelType, Class<? extends Level>>> levelClasses);
}
