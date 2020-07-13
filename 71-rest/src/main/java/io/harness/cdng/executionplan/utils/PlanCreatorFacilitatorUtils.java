package io.harness.cdng.executionplan.utils;

import io.harness.cdng.common.MiscUtils;
import io.harness.facilitator.FacilitatorType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreatorFacilitatorUtils {
  public static String decideTaskFacilitatorType() {
    if (MiscUtils.isNextGenApplication()) {
      return FacilitatorType.TASK_V2;
    }
    return FacilitatorType.TASK;
  }

  public static String decideTaskChainFacilitatorType() {
    if (MiscUtils.isNextGenApplication()) {
      return FacilitatorType.TASK_CHAIN_V2;
    }
    return FacilitatorType.TASK_CHAIN;
  }
}
