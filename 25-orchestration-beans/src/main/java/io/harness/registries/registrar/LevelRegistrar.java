package io.harness.registries.registrar;

import io.harness.ambiance.Level;

import java.util.Set;

public interface LevelRegistrar extends EngineRegistrar<Level> {
  void register(Set<Class<? extends Level>> levelClasses);
}
