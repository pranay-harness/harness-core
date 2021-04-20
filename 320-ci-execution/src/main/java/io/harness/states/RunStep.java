package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.CI)
public class RunStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = RunStepInfo.STEP_TYPE;
}
