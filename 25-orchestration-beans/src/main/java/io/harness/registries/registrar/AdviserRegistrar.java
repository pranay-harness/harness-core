package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Adviser;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;

@OwnedBy(CDC)
public interface AdviserRegistrar extends EngineRegistrar<Adviser> {
  void register(Set<Class<? extends Adviser>> adviserClasses);
}
