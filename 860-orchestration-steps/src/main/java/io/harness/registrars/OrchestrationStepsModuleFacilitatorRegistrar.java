package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.steps.barriers.BarrierFacilitator;
import io.harness.steps.resourcerestraint.ResourceRestraintFacilitator;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationStepsModuleFacilitatorRegistrar {
  public Map<FacilitatorType, Facilitator> getEngineFacilitators(Injector injector) {
    Map<FacilitatorType, Facilitator> engineFacilitators = new HashMap<>();

    engineFacilitators.put(BarrierFacilitator.FACILITATOR_TYPE, injector.getInstance(BarrierFacilitator.class));
    engineFacilitators.put(
        ResourceRestraintFacilitator.FACILITATOR_TYPE, injector.getInstance(ResourceRestraintFacilitator.class));

    return engineFacilitators;
  }
}
