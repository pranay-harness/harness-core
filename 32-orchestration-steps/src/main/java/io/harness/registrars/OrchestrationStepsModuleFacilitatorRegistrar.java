package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorType;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.steps.barriers.BarrierFacilitator;
import io.harness.steps.resourcerestraint.ResourceRestraintFacilitator;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationStepsModuleFacilitatorRegistrar implements FacilitatorRegistrar {
  @Override
  public void register(Set<Pair<FacilitatorType, Class<? extends Facilitator>>> facilitatorClasses) {
    facilitatorClasses.add(Pair.of(BarrierFacilitator.FACILITATOR_TYPE, BarrierFacilitator.class));
    facilitatorClasses.add(Pair.of(ResourceRestraintFacilitator.FACILITATOR_TYPE, ResourceRestraintFacilitator.class));
  }
}
