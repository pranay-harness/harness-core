package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;

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
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<FacilitatorType, Facilitator>> facilitatorClasses) {
    facilitatorClasses.add(
        Pair.of(BarrierFacilitator.FACILITATOR_TYPE, injector.getInstance(BarrierFacilitator.class)));
    facilitatorClasses.add(Pair.of(
        ResourceRestraintFacilitator.FACILITATOR_TYPE, injector.getInstance(ResourceRestraintFacilitator.class)));
  }
}
