package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;

import java.util.Set;

@OwnedBy(CDC)
public interface FacilitatorRegistrar extends EngineRegistrar<Facilitator> {
  void register(Set<Class<? extends Facilitator>> facilitatorClasses);
}
