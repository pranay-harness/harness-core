package io.harness.cdng.executionplan.utils;

import io.harness.facilitator.FacilitatorType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreatorFacilitatorUtils {
  public String decideTaskFacilitatorType() {
    return FacilitatorType.TASK_V2;
  }

  public String decideTaskChainFacilitatorType() {
    return FacilitatorType.TASK_CHAIN_V3;
  }
}
