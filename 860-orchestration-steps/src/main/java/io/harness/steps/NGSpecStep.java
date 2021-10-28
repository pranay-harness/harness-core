package io.harness.steps;

import io.harness.OrchestrationStepTypes;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.steps.common.NGSectionStep;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGSpecStep extends NGSectionStep {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.NG_SPEC_STEP).setStepCategory(StepCategory.STEP).build();
}
