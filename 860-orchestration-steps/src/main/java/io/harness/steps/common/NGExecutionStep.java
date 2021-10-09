package io.harness.steps.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.steps.OrchestrationStepTypes;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGExecutionStep extends NGSectionStepWithRollbackInfo {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.NG_EXECUTION).setStepCategory(StepCategory.STEP).build();
}
