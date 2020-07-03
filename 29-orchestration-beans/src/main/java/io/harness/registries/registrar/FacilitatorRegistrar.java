package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface FacilitatorRegistrar extends EngineRegistrar<FacilitatorType, Facilitator> {
  void register(Set<Pair<FacilitatorType, Class<? extends Facilitator>>> facilitatorClasses);
}
