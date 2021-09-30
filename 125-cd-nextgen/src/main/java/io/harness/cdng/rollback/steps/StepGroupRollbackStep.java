package io.harness.cdng.rollback.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.steps.common.NGSectionStep;

@OwnedBy(HarnessTeam.PIPELINE)
public class StepGroupRollbackStep extends NGSectionStep {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(OrchestrationStepTypes.STEP_GROUP_ROLLBACK_STEP)
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
}
