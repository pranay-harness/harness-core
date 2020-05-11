package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;

@OwnedBy(CDC)
public interface LevelRegistrar extends EngineRegistrar<Level> {
  void register(Set<Class<? extends Level>> levelClasses);
}
