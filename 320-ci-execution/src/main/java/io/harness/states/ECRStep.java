package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.ECRStepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.CI)
public class ECRStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = ECRStepInfo.STEP_TYPE;
}
