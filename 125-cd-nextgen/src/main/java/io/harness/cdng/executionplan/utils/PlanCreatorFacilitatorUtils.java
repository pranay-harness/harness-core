package io.harness.cdng.executionplan.utils;

import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreatorFacilitatorUtils {
  public String decideTaskFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_V2;
  }

  public String decideTaskChainFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_CHAIN_V3;
  }
}
