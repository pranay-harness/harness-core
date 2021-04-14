package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.steps.resourcerestraint.ResourceRestraintFacilitator;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class OrchestrationStepsModuleFacilitatorRegistrar {
  public Map<FacilitatorType, Class<? extends Facilitator>> getEngineFacilitators() {
    Map<FacilitatorType, Class<? extends Facilitator>> engineFacilitators = new HashMap<>();
    engineFacilitators.put(ResourceRestraintFacilitator.FACILITATOR_TYPE, ResourceRestraintFacilitator.class);
    return engineFacilitators;
  }
}
