package io.harness.registries.registrar;

import io.harness.adviser.Adviser;

import java.util.Set;

public interface AdviserRegistrar extends EngineRegistrar<Adviser> {
  void register(Set<Class<? extends Adviser>> adviserClasses);
}
