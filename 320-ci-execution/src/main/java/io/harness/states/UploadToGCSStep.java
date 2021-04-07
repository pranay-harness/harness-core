package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.UploadToGCSStepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.CI)
public class UploadToGCSStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = UploadToGCSStepInfo.STEP_TYPE;
}
