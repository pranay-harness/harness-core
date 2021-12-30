package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.SecurityStepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.STO)
public class SecurityStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = SecurityStepInfo.STEP_TYPE;
}