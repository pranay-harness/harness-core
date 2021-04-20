package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.GCRStepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.CI)
public class GCRStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = GCRStepInfo.STEP_TYPE;
}
