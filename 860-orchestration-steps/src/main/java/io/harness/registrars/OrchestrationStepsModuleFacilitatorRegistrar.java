/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.execution.events.node.facilitate.Facilitator;
import io.harness.steps.approval.ApprovalFacilitator;
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
    engineFacilitators.put(ApprovalFacilitator.FACILITATOR_TYPE, ApprovalFacilitator.class);
    return engineFacilitators;
  }
}
