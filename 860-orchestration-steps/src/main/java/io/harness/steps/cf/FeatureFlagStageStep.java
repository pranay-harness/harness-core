package io.harness.steps.cf;

import io.harness.OrchestrationStepTypes;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.steps.common.NGSectionStep;

public class FeatureFlagStageStep extends NGSectionStep {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.FLAG_STAGE).setStepCategory(StepCategory.STAGE).build();
}
