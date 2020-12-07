package io.harness.pms.sdk.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.registries.Registrar;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface FacilitatorRegistrar extends Registrar<FacilitatorType, Facilitator> {
  void register(Set<Pair<FacilitatorType, Facilitator>> facilitatorClasses);
}
